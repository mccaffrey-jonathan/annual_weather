(ns annual-weather.views
  (:use [hiccup core page]))

(defn index-page []
  (html5
    [:head
     [:title "Hello World"] 
     (include-js "http://d3js.org/d3.v3.min.js"
                 "http://code.jquery.com/jquery-1.11.0.min.js"
                 "js/main.js")]
    [:body
     [:h1 "Hello World"]
     [:div {:class "result"}]]))

