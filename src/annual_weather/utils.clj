(ns annual-weather.utils
  (:require 
    [clojure.pprint :as pp]
    [clojure.data [json :as json]])
  (:use [clj-utils.core]))

(def-> unpack-http-kit-json-res
  deref
  ((fn [x] (pp/pprint x) x))
  :body
  (json/read-str :key-fn keyword))
