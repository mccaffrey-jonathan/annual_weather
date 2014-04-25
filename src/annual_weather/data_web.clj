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
  (:use [annual-weather data geocode utils]
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

(def Bucketed {s/Any {s/Str [s/Int] } })

(def D3-Schema [{:key s/Int
                 :value {s/Str [s/Int] } }])

; TODO is there a builtin for this?
; Also not quite right.
(defn vec-spread [& fns]
  (fn [x]
    (vec (map apply fns (repeat x)))))


(let [df (SimpleDateFormat. "yyyy-MM-dd")]
  (defn parse-ncdc-date [s]
    (.parse df s)))

; TODO for now group by month of year
(defn bucket-date-str [s]
  (.get (doto (Calendar/getInstance)
          (.setTime (parse-ncdc-date s)))
        Calendar/MONTH))

; TODO do we need to merge now?
(defn ncdc-data-seq-to-map [xs]
  (apply merge-with concat
        (for [x xs] {(x :datatype) [(x :value)]})))

; TODO what is the input schema ?
(sm/defn group-results :- Bucketed
  [res] 
  (->> res
       (group-by (comp bucket-date-str :date))
       (fmap ncdc-data-seq-to-map)))

(sm/defn groups-to-d3-style :- D3-Schema
  [gs :- Bucketed]
  (vec (for [[k v] gs] {:key k :value v})))

(def->> data-for-human-location
  (find-nearest-station query-n) ; find a station
  :id 
  (assoc query-n :stationid)
  (query-cdo :data) ; Ask for weather data
  unpack-http-kit-json-res ; parse the response
  :results)

(def mock-data true)

(defn api-handler []
  (if mock-data
    (read-string (slurp "la-data-d3"))
    (-> "San Jose, CA"
        data-for-human-location
        group-results
        groups-to-d3-style)))

; TODO handle errors more pleasantly
; Catch exceptions and serve up a nice response
(defn search-handler [s]
  (if mock-data
    (read-string (slurp "la-data-d3"))
    (-> s
        ((fn [s] (println s) s) )
        data-for-human-location
        group-results
        groups-to-d3-style)))
