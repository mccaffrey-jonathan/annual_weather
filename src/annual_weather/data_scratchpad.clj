(ns annual-weather.data-scratchpad
  (:import 
    java.util.Calendar
    java.text.SimpleDateFormat)
  (:require [clojure.java [io :as io]]
            [clojure.data [json :as json]]
            [clojure
             [pprint :as pp]
             [string :as s]]
            [org.httpkit.client :as http])
  (:use [annual-weather data utils]
        [uncomplicate.fluokitten core jvm]))

(def query-dates
  {:startdate "1995-01-01"
  :enddate   "2005-01-01" })

(def query-data-type-and-set
  {:datatypeid ["EMNT" "EMXT" "MMNT" "MMXT" "MNTM"]
   :datasetid "GHCNDMS"})

(defn query-first-n-results [n]
  {:limit (* 5 12 5)
   :offset 0})

(def query-d
  "For a given city, this dataset appears to be an array of maps, each
  conrtaining one datapoint.  The datapoints move through all
  cateugories for a date, then advance"
  (merge
    query-dates
    query-data-type-and-set))

(def query-n
  "For a given city, this dataset appears to be an array of maps, each
  conrtaining one datapoint.  The datapoints move through all
  cateugories for a date, then advance"
  (merge
    query-d
    (query-first-n-results (* 5 12 10))))

; This one works still
; (pp-res (http/get url opts))

; (pp-res (http/get data-url opts-d))

(def query-l (assoc query-n :stationid "GHCND:AJ000037985"))

; TODO no wifi
; (def prom (query-cdo :data query-l))
; (redir "yearly-sample"
;   (pp-res prom))

; (def monthly-locations (unpack-res (query-cdo :locations orpts-d)))
; (def monthly-datasets (unpack-res (query-cdo :dataset opts-d)))
; (def monthly-datatypes (unpack-res (query-cdo :datatypes opts-d)))
; (def monthly-stations (unpack-res (query-cdo :stations opts)))

; (def zips (query-cdo :locations {:locationcategoryid "ZIP"}))
; Location ID's are represent like ZIP:XXXXX

(def mock-data true)
(def monthly-data 
  (if mock-data
    (read-string (slurp "stable-data"))
    (unpack-res (query-cdo :data query-l))))
; (redir "stable-data"
;         (pp/pprint monthly-data))


; TODO is there a builtin for this?
; Also not quite right.
(defn vec-spread [& fns]
  (fn [x]
    (vec (map apply fns (repeat x)))))


; TODO do we need to merge now?
(defn ncdc-data-seq-to-map [xs]
  (apply merge-with concat
        (for [x xs] {(x :datatype) [(x :value)]})))

(let [df (SimpleDateFormat. "yyyy-MM-dd")]
  (defn parse-ncdc-date [s]
    (.parse df s)))

; TODO for now group by month of year
(defn bucket-date-str [s]
  (.get (doto (Calendar/getInstance)
          (.setTime (parse-ncdc-date s)))
        Calendar/MONTH))

(defn group-results [res]
  (->> res
       (group-by (comp bucket-date-str :date))
       (fmap ncdc-data-seq-to-map)))

(defn json-weather-data []
  (group-results (monthly-data :results)))

; (json-weather-data)
(defn check-location-quality [locstr]
  (mdo [station (-> (query-cdo :stations
                               (assoc query-data-type-and-set
                                      :locationid locstr))
                    unpack-res 
                    :results
                    first
                    just)
        d (-> (query-cdo :data
                   (assoc query-d
                          :limit (* 5 12 10)
                          :offset 0
                          :stationid (station :id)))
              unpack-res 
              :results
              group-results
              just)]
         d))

(check-location-quality "ZIP:95112")

; 
; (redir "locations"
;        (pp/pprint monthly-locations))
; 
; (redir "joined"
;        (pp/pprint
;          (clojure.set/join
;            (monthly-datatypes :results)
;            (monthly-data :results))))

; (redir "stations"
;        (pp/pprint monthly-stations))
       
; (spit "monthly.txt" monthly-data)

; (binding [*out* (java.io.FileWriter. "monthly.dat")]
;   (pp/pprint monthly-data))

;  {"id" "CITY:AR000002",
;    "name" "Buenos Aires, AR",
;    "datacoverage" 0.9896,
;    "mindate" "1908-09-01",
;    "maxdate" "2004-05-01"}

;(redir "trim-monthly" ;       (pp/pprint monthly-stations))
