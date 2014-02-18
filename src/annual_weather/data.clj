(ns annual-weather.data
  (:require [clojure.java [io :as io]]
            [clojure.data [json :as json]]
            [clojure.core.rrb-vector :as fv]
            [clojure
             [pprint :as pp]
             [string :as s]]
            [org.httpkit.client :as http])
  (:use [annual-weather utils]
        [clj-utils.core]))

(def token (s/trim (slurp "ncdc-token")))
(def auth-opts {:headers {"token" token}})

; (def url "http://www.ncdc.noaa.gov/cdo-web/api/v2/locations?datasetid=GHCNDMS")

(def-> pp-res
  unpack-http-kit-json-res
  pp/pprint)

(defn query-cdo
  [endpoint query]
  (let [url (str "http://www.ncdc.noaa.gov/cdo-web/api/v2/"
                 (name endpoint))
        opts (assoc auth-opts :query-params query)
        prom (http/get url opts)]
    ; (print @prom)
    prom))

; TODO async version
(defn query-cdo-full-depaginated-results
  [endpoint query]
  (loop [accum []
         query-offset 1]
    (let [{results :results
           {{:keys [limit offset] cnt :count} :resultset} :metadata
           :as ret}
          (unpack-http-kit-json-res
            (query-cdo endpoint
                       (assoc query :limit 1000 :offset query-offset)))
          grown-results (fv/catvec accum (if results results []))]
      (if (< cnt (+ limit offset))
        grown-results
        (recur grown-results
               (+ limit offset))))))

; NOTE data query without any station qualifiers times out
; Note, data returned is in 10ths of a degree celsius
