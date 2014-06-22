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

(defn find-good-station-near-geocoded
  [q geocoded]
  (debug "find-good-station-near-geocoded q " q " geocoded "geocoded)
  (let [stations (->> geocoded
                      fuzzy-bound-geocode-loc
;                       ((fn [x] 
;                         (info "bound " x)
;                         x))
                      (query-stations q "TODO INCLUDE ADDR"))]
        (find-good-near-geocoded geocoded stations)))

(defn data-for-geocoded-location [geocoded]
  (if-let [st (find-good-station-near-geocoded
                (merge (recent-enough-dates) query-n) geocoded)]
    (do
      (debug "Chose station" (st :name)
                  "with coverage" (st :datacoverage)
                  "located at lat: " (st :latitude) ", lng:" (st :longitude))
      (query-data-d3-style
        (merge
          (assoc query-n 
                 :stationid (st :id))
          (recent-dates-for-station st))))
    (do
      (debug "No stations found!")
      nil)))

(defn data-for-human-location [human-loc]
  (data-for-geocoded-location
    (query-geocode human-loc)))

(def mock-data false)

(defn api-handler []
  (if mock-data
    (read-string (slurp "la-data-d3"))
    (data-for-human-location "San Jose, CA")))

(defn nil-handler [b]
  nil)

; TODO handle errors more pleasantly
; Catch exceptions and serve up a nice response
(defn data-handler [b]
  (if-let [d (data-for-geocoded-location (b :geocoded))] 
    {:body d}))

(defn search-handler [s]
  {:body (if mock-data
           (read-string (slurp "la-data-d3"))
           (do
             (println "search for query '" s "'")
             (data-for-human-location s))) })

