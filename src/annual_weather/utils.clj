(ns annual-weather.utils
  (:require [clojure.data [json :as json]])
  (:use [clj-utils.core]))

(def-> unpack-http-kit-json-res
  deref
  ; ((fn [x] (print x) x))
  :body
  (json/read-str :key-fn keyword))
