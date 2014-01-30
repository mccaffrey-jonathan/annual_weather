(ns annual-weather.data-scratchpad
  (:use annual-weather.data)
  (:require [clojure.java [io :as io]]
            [clojure.data [json :as json]]
            [clojure
             [pprint :as pp]
             [string :as s]]
            [org.httpkit.client :as http]))

;
(def query-d
  "For a given city, this dataset appears to be an array of maps, each
  containing one datapoint.  The datapoints move through all
  categories for a date, then advance"
  {
   ; :startdate "1999-01-01"
   ; :enddate "2000-01-01" 
   :datatypeid ["EMNT" "EMXT" "MMNT" "MMXT" "MNTM"]
   :datasetid "GHCNDMS" })

; This one works still
; (pp-res (http/get url opts))

; (pp-res (http/get data-url opts-d))

(def query-l (assoc query-d :stationid "COOP:010402"))
;

(def prom (query-cdo :data query-d))
(deref prom)

; (def monthly-locations (unpack-res (query-cdo :locations opts-d)))
; (def monthly-datasets (unpack-res (query-cdo :dataset opts-d)))
; (def monthly-datatypes (unpack-res (query-cdo :datatypes opts-d)))
; (def monthly-stations (unpack-res (query-cdo :stations opts)))
;
; (def monthly-data (unpack-res (query-cdo :data query-l)))

; (redir "trim-monthly"
;        (pp/pprint monthly-data))
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

;(redir "trim-monthly"
;       (pp/pprint monthly-stations))

