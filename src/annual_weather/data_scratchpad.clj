(ns annual-weather.data-scratchpad
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
    (read-string (slurp "la-data-d3"))
    (unpack-http-kit-json-res (query-cdo :data query-l))))

(defn api-handler []
  monthly-data)

(defn groups-to-d3-style
  [gs]
  (vec (for [[k v] gs] {:key k :value v})))

   ; monthly-data)
; (redir "stable-data"
;         (pp/pprint monthly-data))

; TODO XXX regenerate data for d3
; (redir "la-data"
;        (pp/pprint
; (->> "Los Angeles, California"
;      (find-nearest-station query-n) ; find a station
;      :id
;      (assoc query-d
;             :limit (* 5 12 10)
;             :offset 0
;             :stationid) 
;      (query-cdo :data) 
;      unpack-http-kit-json-res ; parse the response
;      :results 
;      group-results)
;          ))



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

(def Bucketed {s/Any {s/Str [s/Int] } })

(sm/defn group-results :- Bucketed
  [res] 
  (->> res
       (group-by (comp bucket-date-str :date))
       (fmap ncdc-data-seq-to-map)))

(sm/defn histogram-bucket-quality :- {s/Int s/Int}
  [gs :- Bucketed]
  (apply merge-with +
         (for [[bucket g] gs]
           {(count (filter (fn [[k v]] (not (empty? v))) g)) 1})))

(defn json-weather-data []
  (group-results (monthly-data :results)))

(defn just-if-not-nil
  [x]
  (if (nil? x) nil (just x)))

; (json-weather-data)
; (defn check-quality [field value]
;   (mdo [station (-> (query-cdo :stations
;                                (assoc query-data-type-and-set
;                                       field value))
;                     unpack-http-kit-json-res 
;                     :results
;                     first
;                     just-if-not-nil)
;         d (-> (query-cdo :data
;                    (assoc query-d
;                           :limit (* 5 12 10)
;                           :offset 0
;                           :stationid (station :id)))
;               unpack-http-kit-json-res 
;               :results
;               group-results
;               histogram-bucket-quality
;               just-if-not-nil)]
;          d))
(defn check-quality [field value]
  (-> (query-cdo :data
                 (assoc query-d
                        :limit (* 5 12 10)
                        :offset 0
                        field value))
      unpack-http-kit-json-res 
      :results
      group-results
      histogram-bucket-quality))

; (unpack-http-kit-json-res
;   (query-cdo :locations
;              {:locationcategoryid "CITY"}))
;
;


; (redir "la-stations"
;        (pp/pprint
;          (unpack-http-kit-json-res
;            (query-cdo
;              :stations
;              {:extent (->
;                         "Los Angeles, California"
;                         query-geocode 
;                         unpack-http-kit-json-res
;                         (get-in [:results 0] )
;                         fuzzy-bound-geocode-loc
;                         window-to-gmaps-url)}))))
; (check-quality
;   :stationid
;   (:id (find-nearest-station query-n "Los Angeles, California")))

;  (redir "long-city-and-vacation-quality"
;          (with-open [in (io/reader
;                           "/home/jmccaf/annual-weather/cities-and-vacations")]
;            (doseq [ln (line-seq in)]
;              (println [ln
;                        (try 
;                        (check-quality
;                          :stationid
;                          (:id (find-nearest-station query-n ln)))
;                          (catch Exception e nil))]))))

; ( find-nearest-station query-n "Los Angeles, California")

; (query-cdo-full-depaginated-results
;   :stations
;   {:extent (->
;              "Los Angeles, California"
;              query-geocode 
;              unpack-http-kit-json-res
;              (get-in [:results 0] )
;              fuzzy-bound-geocode-loc
;              window-to-gmaps-url)})

; (redir "zip-code-quality"
;        (with-open [zips-in (io/reader
;                              "/home/jmccaf/annual-weather/zip-code-list")]
;          (doseq [zip (line-seq zips-in)]
;            (println [zip (check-location-quality (str "ZIP:" zip))]))))




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
