(ns annual-weather.cdo
  (:import 
    java.util.Calendar
    java.text.SimpleDateFormat)
  (:require [clojure.java [io :as io]]
            [clojure.data [json :as json]]
            [clojure.core.rrb-vector :as fv]
            [clojure
             [pprint :as pp]
             [string :as s]]
            [org.httpkit.client :as http]
            [schema
             [core :as sc]
             [macros :as sm] ]
            [throttler.core :refer [ throttle-fn ]])
  (:use [annual-weather utils]
        [clj-utils.core]
        [uncomplicate.fluokitten core jvm]))

(def token (s/trim (slurp "ncdc-token")))
(def auth-opts {:headers {"token" token}})

; (def url "http://www.ncdc.noaa.gov/cdo-web/api/v2/locations?datasetid=GHCNDMS")

(def-> pp-res
  unpack-http-kit-json-res
  pp/pprint)

(defn query-unthrottled
  [endpoint q]
  (let [url (str "http://www.ncdc.noaa.gov/cdo-web/api/v2/"
                 (name endpoint))
        opts (assoc auth-opts :query-params q)
        prom (http/get url opts)]
    prom))

; NCDC allows 1000 per day.  Give it some breathing room
(def query (throttle-fn query-unthrottled 750 :day 250))

; TODO async version
(defn query-full-depaginated-results
  [endpoint q]
  (loop [accum []
         query-offset 1]
    (let [{results :results
           {{:keys [limit offset] cnt :count} :resultset} :metadata
           :as ret}
          (unpack-http-kit-json-res
            (query endpoint
                   (assoc q :limit 1000 :offset query-offset)))
          grown-results (fv/catvec accum (if results results []))]
      (if (< cnt (+ limit offset))
        grown-results
        (recur grown-results
               (+ limit offset))))))

(defn query-stations-uncached [q gmaps-extent]
  (query-full-depaginated-results
    :stations
    (assoc q :extent gmaps-extent)))

(def Bucketed {sc/Any {sc/Str [sc/Int] } })

(def D3-Schema [{:key sc/Int :value {sc/Str [sc/Int] }}])

(let [df (SimpleDateFormat. "yyyy-MM-dd")]
  (defn parse-ncdc-date [s]
    (.parse df s)))

; TODO for now group by month of year
(defn bucket-date-str [s]
  (.get (doto (Calendar/getInstance)
          (.setTime (parse-ncdc-date s)))
        Calendar/MONTH))

; TODO is there a builtin for this?
; Also not quite right.
(defn vec-spread [& fns]
  (fn [x]
    (vec (map apply fns (repeat x)))))

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

; NOTE data query without any station qualifiers times out
; Note, data returned is in 10ths of a degree celsius

(defn update-values [m f & args]
  (reduce (fn [r [k v]] (assoc r k (apply f v args))) {} m))
