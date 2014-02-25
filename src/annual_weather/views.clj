(ns annual-weather.views
  (:use [hiccup core page]))

(defn index-page []
  (html5
    [:head
     [:title "Hello World"] 
     [:link {:rel "stylesheet/less"
             :type "text/css"
             :href "less/chart.less"}]
     (include-js "http://d3js.org/d3.v3.min.js"
                 "http://code.jquery.com/jquery-1.11.0.min.js"
                 ; TODO don't use client-side LESS in production
                 "http://cdnjs.cloudflare.com/ajax/libs/less.js/1.6.3/less.min.js"
                 "js/main.js")]
    [:body
     [:h1 "Hello World"]
     [:div {:class "result"}]]))

