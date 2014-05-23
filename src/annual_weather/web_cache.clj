(ns annual-weather.web-cache
  (:import java.util.concurrent.TimeUnit)
  (:require 
    [annual-weather [cdo :as cdo]
                    [geocode :as geocode]]
    [clojure.java [io :as io]]
    [clojure.data [json :as json]]
    [clojure.core.rrb-vector :as fv]
    [clojure [pprint :as pp]]
    [monger core collection [operators :as op]]
    [org.httpkit.client :as http]
    [schema [core :as s]
            [macros :as sm]])
  (:use [annual-weather utils]
        [clj-utils.core]))

(defn db-init []
  (monger.core/connect!)
  (monger.core/use-db! "annual-weather")
  (monger.collection/ensure-index "geocodeWebCache"
                                  (array-map :user-addr 1)
                                  {:expireAfterSeconds 
                                   (.toSeconds TimeUnit/DAYS 365)})
  (monger.collection/ensure-index "d3StyleDataWebCache"
                                  (array-map :query-rest 1)
                                  {:expireAfterSeconds 
                                   (.toSeconds TimeUnit/DAYS 365)})
  (monger.collection/ensure-index "stationWebCache"
                                  (array-map :longitudeLatitude "2dsphere" 
                                             :query-rest 1)
                                  {:expireAfterSeconds 
                                   (.toSeconds TimeUnit/DAYS 365)}))

(def write-db true)
(def read-db true)
(def read-uc true)
(if (or read-db write-db) (db-init))

(sm/defn query-geocode :- geocode/Geocoded
  "TODO is there a structure of cached reads, cache writes, and query
  That we can use to pull out the specifics from the control flow ?
  TODO log or store cache statistics !
  But this works !"
  [q :- s/Str] 
  (or (and read-db
           (some->> q
             (array-map :user-addr)
             (monger.collection/find-one-as-map "geocodeWebCache")
             :geocoded))
      (let [geocoded (if read-uc
                       (geocode/query-geocode-uncached q))]
        (pp/pprint geocoded)
        (if geocoded
          (do
            (if write-db (monger.collection/insert "geocodeWebCache"
              {:user-addr q
               :geocoded geocoded }))
            geocoded)))))

(sm/defn query-data-d3-style-uncached [q] :- cdo/D3-Schema
  (->> q
      (cdo/query :data) ; Ask for weather data
      unpack-http-kit-json-res ; parse the response
      :results
      cdo/group-results
      cdo/groups-to-d3-style))

(sm/defn query-data-d3-style [q] :- cdo/D3-Schema
  (let [maybe-write-db (fn [q-date q-rest data]
         (if write-db 
          (monger.collection/insert "d3StyleDataWebCache"
             {:query-date q-date :query-rest q-rest :data data})))
        maybe-read-db (fn [q-rest]
          (and read-db
                   (some->> q-rest
                            (array-map :query-rest)
                            (monger.collection/find-one-as-map "d3StyleDataWebCache")
                            :data)))
        [q-date q-rest] (group-map-by-keys q [:startdate :enddate])]
    (or (maybe-read-db q-rest)
        (if read-uc
          (if-let [data (query-data-d3-style-uncached q)]
            (do (maybe-write-db q-date q-rest data) data))))))

; TODO write and read query to DB, and add secondary index
(defn query-stations [q extent]
  (let [maybe-write-db (fn [q-rest stations]
         (if write-db 
           (doseq [station stations]
            (monger.collection/insert "stationWebCache"
                {:longitudeLatitude
                 [(station :longitude) (station :latitude)]
                 :query-rest q-rest
                 :station station}))))
        maybe-read-db (fn [q-rest extent]
          (and read-db
               (some->>
                 (monger.collection/find-maps "stationWebCache"
                  {:longitudeLatitude
                 {op/$geoWithin
                  {"$box"
                   [[(.getLeftLongitude extent) (.getMinLatitude extent)]
                    [(.getRightLongitude extent) (.getMaxLatitude extent)]] }}
                   :query-rest q-rest })
                 (map :station))))
        [q-date q-rest] (group-map-by-keys 
                          (dissoc q :limit :offset :sortorder :sortfield)
                          [:startdate :enddate])]
    (or (maybe-read-db q-rest extent)
        (if read-uc
          (if-let [stations (cdo/query-stations-uncached q (geocode/window-to-gmaps-url extent))]
            (do (maybe-write-db q-rest stations) stations))))))

