(ns annual-weather.data
  (:require [clojure.java [io :as io]]
            [clojure.data [json :as json]]
            [clojure
             [pprint :as pp]
             [string :as s]]
            [org.httpkit.client :as http]))

; generic stuff
; TODO metadata and docstr
(defmacro def->
  [fn-name & fn-body]
  `(defn ~fn-name
     [x#]
     (-> x# ~@fn-body)))

; http://briancarper.net/blog/495/
(defmacro redir [filename & body]
  `(binding [*out* (clojure.java.io/writer ~filename)] ~@body)) 

(def token (s/trim (slurp "ncdc-token")))
(def auth-opts {:headers {"token" token}})

; (def url "http://www.ncdc.noaa.gov/cdo-web/api/v2/locations?datasetid=GHCNDMS")

(def-> unpack-res
  deref
  (get-in [:body])
  (json/read-str :key-fn keyword))

(def-> pp-res
  unpack-res
  pp/pprint)

(defn query-cdo
  [endpoint query]
  (let [url (str "http://www.ncdc.noaa.gov/cdo-web/api/v2/"
                 (name endpoint))
        opts (assoc auth-opts :query-params query)
        prom (http/get url opts)]
    ; (print @prom)
    prom))

; NOTE data query without any station qualifiers times out

; Note, data returned is in 10ths of a degree celsius
