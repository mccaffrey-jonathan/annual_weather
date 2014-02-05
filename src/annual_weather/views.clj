(ns annual-weather.views
  (:use [hiccup core page]))

(defn index-page []
  (html5
    [:head
      [:title "Hello World"] 
     (include-js "http://d3js.org/d3.v3.min.js"
                 
                 )]
    [:body
      [:h1 "Hello World"]]))
