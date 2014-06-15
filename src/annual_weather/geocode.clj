(ns annual-weather.geocode
  (:import 
    com.javadocmd.simplelatlng.LatLng
    com.javadocmd.simplelatlng.LatLngTool
    com.javadocmd.simplelatlng.util.LengthUnit
    com.javadocmd.simplelatlng.window.RectangularWindow)
  (:require [clojure [pprint :as pp]]
            [org.httpkit.client :as http]
            [schema [core :as s]
                    [macros :as sm]]
            [throttler.core :refer [throttle-fn]])
  (:use [annual-weather cdo utils]
        [clj-utils.core]))

(def-> unpack-geocode
  deref)

(defn query-geocode-uncached-unthrottled [q]
  (let [res (unpack-http-kit-json-res
              (http/get
                "http://maps.googleapis.com/maps/api/geocode/json"
                {:query-params
                 {:address q
                  :sensor false }}))]
    (if res (get-in res [:results 0]))))

(def query-geocode-uncached 
  (throttle-fn query-geocode-uncached-unthrottled 750 :day 250))

; TODO does this work outside of NW demi-sphere?
(defn window-to-gmaps-url
  "Returns a string of the form lat_lo,lng_lo,lat_hi,lng_hi for this
  bounds, where lo corresponds to the southwest corner of the bounding
  box, while hi corresponds to the northeast corner of that box."
  [window]
  (str
    (.getMinLatitude window) ","
    (.getLeftLongitude window) ","
    (.getMaxLatitude window) ","
    (.getRightLongitude window)))

(def loc-padding-km 10)

(defn map-to-latlng
  [{:keys [lat lng] :as latlng}]
  (LatLng. lat lng))

(defn bounds-to-window 
    [{:keys [northeast southwest]}]
  (RectangularWindow.
    (map-to-latlng northeast)
    (map-to-latlng southwest)))

; TODO just works in western hemisphere atm!
(defn union-window
  [& more]
  (RectangularWindow.
    (LatLng. 
      (reduce max (map #(.getMaxLatitude %) more))
      (reduce min (map #(.getRightLongitude %) more)))
    (LatLng. 
      (reduce min (map #(.getMinLatitude %) more))
      (reduce max (map #(.getLeftLongitude %) more)))))

(defn fuzzy-bound-geocode-loc
  [{{geocode-bounds-map :bounds
     geocode-loc :location} :geometry
    :as geo}]
  (let [padded-window
        (RectangularWindow.
          (map-to-latlng geocode-loc)
          (* 2 loc-padding-km)
          (* 2 loc-padding-km)
          LengthUnit/KILOMETER)]
    (if (nil? geocode-bounds-map)
      padded-window
      (union-window
        padded-window
        (bounds-to-window geocode-bounds-map)))))

(def Geocode-LatLng {s/Keyword s/Num})
(def Geocode-Rect {:northeast Geocode-LatLng
                   :southwest Geocode-LatLng})

(def Geocoded
  {:address_components
    [{:long_name s/Str
      :short_name s/Str
      :types [s/Str]}]
   :formatted_address s/Str
   :geometry
   {:bounds Geocode-Rect
    :location Geocode-LatLng
    :location_type s/Str
    :viewport Geocode-Rect
    :types [s/Str]}})

(def skaneateles-example-data
  {:results
   [{:address_components
     [{:long_name "Skaneateles",
       :short_name "Skaneateles",
       :types ["locality" "political"]}
      {:long_name "Skaneateles",
       :short_name "Skaneateles",
       :types ["administrative_area_level_3" "political"]}
      {:long_name "Onondaga",
       :short_name "Onondaga",
       :types ["administrative_area_level_2" "political"]}
      {:long_name "New York",
       :short_name "NY",
       :types ["administrative_area_level_1" "political"]}
      {:long_name "United States",
       :short_name "US",
       :types ["country" "political"]}
      {:long_name "13152", :short_name "13152", :types ["postal_code"]}],
     :formatted_address "Skaneateles, NY 13152, USA",
     :geometry
     {:bounds
      {:northeast {:lat 42.9589969, :lng -76.40928},
       :southwest {:lat 42.936439, :lng -76.4448259}},
      :location {:lat 42.947011, :lng -76.42910169999999},
      :location_type "APPROXIMATE",
      :viewport
      {:northeast {:lat 42.9589969, :lng -76.40928},
       :southwest {:lat 42.936439, :lng -76.4448259}}},
     :types ["locality" "political"]}],
   :status "OK"})

(-> skaneateles-example-data
    (get-in [:results 0] )
    fuzzy-bound-geocode-loc
    window-to-gmaps-url)

(defn find-good-near-geocoded
  [{{{loc-lat :lat loc-lng :lng} :location} :geometry} stations]
  (letfn [(distance-to-location [{stat-lat :latitude stat-lng :longitude}]
            (LatLngTool/distance
              (LatLng. stat-lat stat-lng)
              (LatLng. loc-lat loc-lng)
              LengthUnit/KILOMETER))]
    ; Sort by datacoverage rather than distance
    ; TODO jmccaffrey: combine the 2 metrics
    (first (sort-by :datacoverage stations))
    ;(first (sort-by distance-to-location stations))
    ))


; (-> "Skaneateles NY"
;     query-geocode
;     unpack-http-kit-json-res
;     ; (json/read-str :key-fn keyword)
;     pp/pprint)
