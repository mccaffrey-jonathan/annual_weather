(ns annual-weather.web-cache
  (:import java.util.concurrent.TimeUnit
           org.joda.time.DateTime)
  (:require 
    [annual-weather [cdo :as cdo]
                    [geocode :as geocode]]
    [clojure.java [io :as io]]
    [clojure.data [json :as json]]
    [clojure.core.rrb-vector :as fv]
    [clojure [pprint :as pp]]
    environ.core
    [monger
     collection
     command
     conversion
     core
     joda-time
     [operators :as op]]
    [org.httpkit.client :as http]
    [schema [core :as s]
            [macros :as sm]])
  (:use [annual-weather utils]
        [clojure.tools.logging]
        [clj-utils.core]))

(defn connect []
  (if-let [mongo-uri (environ.core/env :mongohq-url)]
    (monger.core/connect-via-uri mongo-uri)
    ; Else, default connection
    (let [conn (monger.core/connect)]
      {:conn conn :db (monger.core/get-db conn, "annual-weather")})))

(def connected-db (atom nil))
(defn init-db []
  (let [{:keys [conn db]} (connect)]
    (reset! connected-db db)
    (monger.collection/ensure-index @connected-db "geocodeWebCache"
                                    (array-map :user-addr 1))
    (monger.collection/ensure-index @connected-db "geocodeWebCache"
                                    (array-map :created-at 1)
                                    {:expireAfterSeconds
                                     (.toSeconds TimeUnit/DAYS 365)})

    (monger.collection/ensure-index @connected-db "stationWebCache"
                                    (array-map :longitudeLatitude "2dsphere" 
                                               :query-rest 1))
    (monger.collection/ensure-index @connected-db "stationWebCache"
                                    (array-map :created-at 1)
                                    {:expireAfterSeconds 
                                     (.toSeconds TimeUnit/DAYS 365)})

    (monger.collection/ensure-index @connected-db "d3StyleDataWebCache"
                                    (array-map :query-rest 1))
    (monger.collection/ensure-index @connected-db "d3StyleDataWebCache"
                                    (array-map :created-at 1)
                                    {:expireAfterSeconds 
                                     (.toSeconds TimeUnit/DAYS 365)})))

(def max-db-file-size (* 256 1000 1000))

(defn insert-if-room [coll doc]
 (if (< (
         (monger.conversion/from-db-object
           (monger.command/db-stats @connected-db)
           true)
         :fileSize) 
        max-db-file-size)
    (monger.collection/insert @connected-db coll doc)))

(def log-caching false)

(def write-db true)
(def read-db true)
(def read-uc true)
(if (or read-db write-db) (init-db))

(defn success-log
  [tag x]
  (if log-caching (info "read" tag "from db successfully"))
  x)

(sm/defn query-geocode :- geocode/Geocoded
  [q :- s/Str] 
  (letfn [(maybe-write-db [q geocoded] 
            (if log-caching (info "if" write-db "write geocode data"))
            (if write-db 
              (insert-if-room "geocodeWebCache"
                            {:user-addr q
                             :geocoded geocoded 
                             :created-at (DateTime. )})))
          (maybe-read-db [q]
            (if log-caching (info "if" read-db "try read geocode data"))
            (if read-db
              (some->> q
                       (array-map :user-addr)
                       (monger.collection/find-one-as-map @connected-db "geocodeWebCache")
                       :geocoded
                       (success-log "geocoded"))))]

  (or (maybe-read-db q)
    (if read-uc
      (if-let [geocoded (geocode/query-geocode-uncached q)]
        (do
          (info "read uc geocoding")
          (maybe-write-db q geocoded)
          geocoded))))))

(sm/defn query-data-d3-style-uncached [q] :- cdo/D3-Schema
  (->> q
      ; ((fn [x] (info q) q))
      (cdo/query :data) ; Ask for weather data
      ; ((fn [x] (info @x) x))
      unpack-http-kit-json-res ; parse the response
      :results
      cdo/group-results
      cdo/groups-to-d3-style))

(sm/defn query-data-d3-style [q] :- cdo/D3-Schema
  (let [maybe-write-db (fn [q-date q-rest data]
         (if log-caching (info "if" write-db "write d3 data"))
         (if write-db 
          (insert-if-room "d3StyleDataWebCache"
             {:query-date q-date
              :query-rest q-rest
              :data data
              :created-at (DateTime. )})))
        maybe-read-db (fn [q-rest]
          (if log-caching (info "if" read-db "try read d3 data"))
          (and read-db
                   (some->> q-rest
                            (array-map :query-rest)
                            (monger.collection/find-one-as-map @connected-db "d3StyleDataWebCache")
                            :data
                            (success-log "data"))))
        [q-date q-rest] (group-map-by-keys q [:startdate :enddate])]
    (or (maybe-read-db q-rest)
        (if read-uc
          (if-let [data (query-data-d3-style-uncached q)]
            (do 
              (if log-caching (info "read uc d3 data"))
              (maybe-write-db q-date q-rest data)
              data))))))

; TODO write and read query to DB, and add secondary index
(defn query-stations [q extent]
  (let [maybe-write-db (fn [q-rest stations]
         (if log-caching (info "if " write-db " write stations"))
         (if write-db 
           (doseq [station stations]
            (insert-if-room "stationWebCache"
                {:longitudeLatitude
                 [(station :longitude) (station :latitude)]
                 :query-rest q-rest
                 :station station
                 :created-at (DateTime. ) }))))
        maybe-read-db (fn [q-rest extent]
          (if log-caching (info "if" read-db "try read stations"))
          (and read-db
               (some->>
                 (monger.collection/find-maps @connected-db "stationWebCache"
                  {:longitudeLatitude
                 {op/$geoWithin
                  {"$box"
                   [[(.getLeftLongitude extent) (.getMinLatitude extent)]
                    [(.getRightLongitude extent) (.getMaxLatitude extent)]] }}
                   :query-rest q-rest })
                 (map :station)
                 not-empty
                 (success-log "station"))))
        [q-date q-rest] (group-map-by-keys 
                          (dissoc q :limit :offset :sortorder :sortfield)
                          [:startdate :enddate])]
    (or (maybe-read-db q-rest extent)
        (if read-uc
          (if-let [stations (cdo/query-stations-uncached q (geocode/window-to-gmaps-url extent))]
              (do 
                (if log-caching
                  (info "read uc station data"))
                (maybe-write-db q-rest stations)
                stations))))))

