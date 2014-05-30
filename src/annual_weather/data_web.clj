(ns annual-weather.data-web
  (:import 
    java.util.Calendar
    java.text.SimpleDateFormat)
  (:require [clojure.java [io :as io]]
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
        [uncomplicate.fluokitten core jvm]))

(def query-dates
  {:startdate "1995-01-01"
  :enddate   "2005-01-01" })

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
    query-dates
    query-d
    (query-first-n-results (* 5 12 10))))

(def query-l (assoc query-n :stationid "GHCND:AJ000037985"))

(defn find-nearest-station
  [q addr]
  (let [geocoded-addr (query-geocode addr)
        stations (->> geocoded-addr
                      fuzzy-bound-geocode-loc
                      (query-stations q))]
        (find-closest-to-geocode geocoded-addr stations)))

(def->> data-for-human-location
  (find-nearest-station query-n) ; find a station
  :id 
  (assoc query-n :stationid)
  query-data-d3-style)

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

