(ns annual-weather.city-list
  (:require 
    [annual-weather [web-cache :as web-cache]
                    [data-web :as data-web]]
    [clojure.java.io :as io]
            [clojure.string :as string])
  (:use [clojure.tools.logging]))

; The main 'geoname' table has the following fields :
; ---------------------------------------------------
; geonameid         : integer id of record in geonames database
; name              : name of geographical point (utf8) varchar(200)
; asciiname         : name of geographical point in plain ascii characters, varchar(200)
; alternatenames    : alternatenames, comma separated, ascii names automatically transliterated, convenience attribute from alternatename table, varchar(8000)
; latitude          : latitude in decimal degrees (wgs84)
; longitude         : longitude in decimal degrees (wgs84)
; feature class     : see http://www.geonames.org/export/codes.html, char(1)
; feature code      : see http://www.geonames.org/export/codes.html, varchar(10)
; country code      : ISO-3166 2-letter country code, 2 characters
; cc2               : alternate country codes, comma separated, ISO-3166 2-letter country code, 60 characters
; admin1 code       : fipscode (subject to change to iso code), see exceptions below, see file admin1Codes.txt for display names of this code; varchar(20)
; admin2 code       : code for the second administrative division, a county in the US, see file admin2Codes.txt; varchar(80) 
; admin3 code       : code for third level administrative division, varchar(20)
; admin4 code       : code for fourth level administrative division, varchar(20)
; population        : bigint (8 byte int) 
; elevation         : in meters, integer
; dem               : digital elevation model, srtm3 or gtopo30, average elevation of 3''x3'' (ca 90mx90m) or 30''x30'' (ca 900mx900m) area in meters, integer. srtm processed by cgiar/ciat.
; timezone          : the timezone id (see file timeZone.txt) varchar(40)
; modification date : date of last modification in yyyy-MM-dd format

(defn is-us [g]
  (= (g :country-code) "US"))

(defn tab-split [ln]
  (vec (string/split ln #"\t")))

(defn parse-country-code-to-kv [ln]
  ; Reject comment lines
  (if (.startsWith ln "#") nil
;ISO	ISO3	ISO-Numeric	fips	Country	Capital	Area(in sq km)	Population	Continent	tld	CurrencyCode	CurrencyName	Phone	Postal Code Format	Postal Code Regex	Languages	geonameid	neighbours	EquivalentFipsCode
    ;
    (let [v (tab-split ln)]
      [{:iso (v 0)}
       {:name (v 4)}])))

(defn parse-admin1-to-kv [ln]
  "Returns nil for mal-formed input"
  (let [v (tab-split ln)
        fst (vec (string/split (v 0) #"\."))]
    (if (not= (count fst) 2) 
      nil
      [{:country-code (fst 0)
        :admin1-code (fst 1)}
       {:name (v 1)}])))

(defn parse-geoname-to-kv [country-map admin-map ln]
  (let [toks (vec (string/split ln #"\t"))
        country-admin1 {:country-code (nth toks 8)
                        :admin1-code (nth toks 10)}]
    (merge 
      country-admin1
      {:name (nth toks 1)
       :latitude (nth toks 4)
       :longitude (nth toks 5)
       :feature-class (nth toks 6)
       :feature-code (nth toks 7)
       :country-name (get-in country-map
                             [{:iso (country-admin1 :country-code)} :name])
       :admin1-name (get-in admin-map
                            [country-admin1 :name])
       :population (nth toks 14) })))

(defn geoname-str [g]
  (if (is-us g)
    (format "%s, %s" (g :name) (g :admin1-name))
    (format "%s, %s" (g :name) (g :country-name))))

(defn largest-us-cities [n coll]
  (take n
        (sort-by #(Integer/parseInt (:population %)) >
                 (filter is-us coll))))

(defn process-lines [country-lns admin1-lns city-lns ]
  (let [country-map (into {}
                          (filter identity
                                  (map parse-country-code-to-kv country-lns)))
        admin-map (into {} 
                        (filter identity
                                (map parse-admin1-to-kv admin1-lns)))
        pps (map 
              (partial parse-geoname-to-kv country-map admin-map)
              city-lns)]
    (doseq [[i geo] (map-indexed 
                      vector
                      (largest-us-cities 1000 pps))
            :let [s (geoname-str geo)]]
      (try 
        (do
          (info "Pulling annual weather for" s "which is #" i)
          (data-web/data-for-human-location s)
          (info "DB so far " (web-cache/cachings-stats)))
        (catch Exception e (warn e "error for city" s)))
      ;(Thread/sleep (* 5 60 1000))
      )))

(defn -main [countryname admin1name cityname]
  (with-open [countryrdr (io/reader countryname)
              admin1rdr (io/reader admin1name)
              cityrdr (io/reader cityname)]
    (process-lines
      (line-seq countryrdr)
      (line-seq admin1rdr)
      (line-seq cityrdr))))

