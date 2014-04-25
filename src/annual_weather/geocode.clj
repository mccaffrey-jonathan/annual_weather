(ns annual-weather.geocode
  (:import 
    com.javadocmd.simplelatlng.LatLng
    com.javadocmd.simplelatlng.LatLngTool
    com.javadocmd.simplelatlng.util.LengthUnit
    com.javadocmd.simplelatlng.window.RectangularWindow)
  (:require [clojure [pprint :as pp]]
            [org.httpkit.client :as http])
  (:use [annual-weather utils data]
        [clj-utils.core]))

(def-> unpack-geocode
  deref)

(defn query-geocode-uncached [q]
  (http/get
    "http://maps.googleapis.com/maps/api/geocode/json"
    {:query-params
     {:address q
      :sensor false }}))

(def query-geocode query-geocode-uncached)

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

(defn find-closest-to-geocode 
  [{{{loc-lat :lat loc-lng :lng} :location} :geometry} stations]
  (letfn [(distance-to-location [{stat-lat :latitude stat-lng :longitude}]
            (LatLngTool/distance
              (LatLng. stat-lat stat-lng)
              (LatLng. loc-lat loc-lng)
              LengthUnit/KILOMETER))]
    (first (sort-by distance-to-location stations))))

(defn find-nearest-station
  [query addr]
  (let [geocoded (-> addr
                     query-geocode
                     unpack-http-kit-json-res)
        geocode-loc (get-in geocoded [:results 0])
        stations (->> geocode-loc
                      fuzzy-bound-geocode-loc
                      window-to-gmaps-url
                      (query-stations query))]
        (find-closest-to-geocode geocode-loc stations)))

; (-> "Skaneateles NY"
;     query-geocode
;     unpack-http-kit-json-res
;     ; (json/read-str :key-fn keyword)
;     pp/pprint)
