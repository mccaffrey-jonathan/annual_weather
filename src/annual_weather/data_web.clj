(ns annual-weather.data-web
  (:import 
    java.util.Calendar
    java.text.SimpleDateFormat)
  (:require [clj-time 
             [core :as t]
             [format :as f] ]
            [clojure.java [io :as io]]
            [clojure.data [json :as json]]
            [clojure
             [pprint :as pp]
             [string]]
            [org.httpkit.client :as http]
            [schema
             [core :as s]
             [macros :as sm] ]
            )
  (:use [annual-weather cdo geocode utils web-cache]
        [clj-utils.core]
        [clojure.tools.logging]
        [uncomplicate.fluokitten core jvm]))

(def query-d
  {:datatypeid ["EMNT" "EMXT" "MMNT" "MMXT" "MNTM"]
   :datasetid "GHCNDMS"})

(defn query-first-n-results [n]
  ; TODO why does this lie
  {:limit (* 5 12 10)
   :offset 0})

(def query-n
  "For a given city, this dataset appears to be an array of maps, each
  conrtaining one datapoint.  The datapoints move through all
  cateugories for a date, then advance"
  (merge
    query-d
    (query-first-n-results (* 5 12 10))))

(defn recent-enough-dates []
  "We'll accept data back to 1990, seems pretty recent to me ?
  TODO, include dates of data used in response"
  {:startdate "1990-01-01"
   :enddate  (f/unparse (f/formatters :date) (t/now))})

(defn recent-dates-for-station [st]
  (let [fmt (f/formatters :date) ]
     {:startdate (f/unparse fmt
                            (t/minus (f/parse fmt (st :maxdate))
                                     (t/years 10)))
      :enddate  (st :maxdate)}))

(def should-log true)

(defn find-good-station-near
  [q addr]
  (let [geocoded-addr (query-geocode addr)
        stations (->> geocoded-addr
                      fuzzy-bound-geocode-loc
                      (query-stations q addr))]
        (find-good-near-geocode geocoded-addr stations)))

(defn data-for-human-location [human-loc]
  (let [st (find-good-station-near
             (merge (recent-enough-dates) query-n) human-loc)] 
    (if should-log (info "Chose station" (st :name)
                         "with coverage" (st :datacoverage)
                         "located at lat: " (st :latitude) ", lng:" (st :longitude)))
    (query-data-d3-style
      (merge
        (assoc query-n 
               :stationid (st :id))
        (recent-dates-for-station st)))))

(def mock-data false)

(defn api-handler []
  (if mock-data
    (read-string (slurp "la-data-d3"))
    (data-for-human-location "San Jose, CA")))

; TODO handle errors more pleasantly
; Catch exceptions and serve up a nice response
(defn search-handler [s]
  (if mock-data
    (read-string (slurp "la-data-d3"))
    (do
       (println "search for query '" s "'")
       (data-for-human-location s))))

