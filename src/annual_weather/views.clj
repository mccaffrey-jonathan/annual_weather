(ns annual-weather.views
  (:require environ.core)
  (:use [hiccup core page]))

(def google-maps-api-key
  (environ.core/env :google-maps-api-key))

(defn search-page []
  (html5
    [:head
     [:meta {:http-equiv "Content-Type"
             :content "text/html;charset=utf-8"}]
     (include-css
       "http://netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css")
     (include-js
       "http://cdnjs.cloudflare.com/ajax/libs/jquery/2.1.1/jquery.min.js"
       "http://netdna.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js"
       "js/search-page.js")
     ]
    [:body
     [:input 
      {:type "search"
       :id "place-search-bar"
       :class "form-control"
       :placeholder "Enter your place" }]
     [:button 
      {:type "submit"
       :id "place-search-submit"
       :class "btn btn-primary"}
      "Search"]]))

(defn chart-page []
  (html5
    [:head

     [:meta {:http-equiv "Content-Type"
             :content "text/html;charset=utf-8"}]
     [:title "Hello Worl"] 
     [:link {:rel "stylesheet/less"
             :type "text/css"
             :href "less/main.less"}]
     [:link {:rel "stylesheet/less"
             :media "print"
             :type "text/css"
             :href "less/print.less"}]
     [:link {:rel "stylesheet/less"
             :media "screen"
             :type "text/css"
             :href "less/screen.less"}]
     [:link {:rel "stylesheet/less"
             :type "text/css"
             :href "less/spinner.less"}]
     (include-js ; "http://d3js.org/d3.v3.min.js"
                 "http://cdnjs.cloudflare.com/ajax/libs/d3/3.4.8/d3.min.js"
                 ; "http://code.jquery.com/jquery-1.11.0.min.js"
                 "http://cdnjs.cloudflare.com/ajax/libs/jquery/2.1.1/jquery.min.js"
                 ; TODO don't use client-side LESS in production
                 "http://cdnjs.cloudflare.com/ajax/libs/less.js/1.6.3/less.min.js"
                 ; "js/async.js"
                 "http://cdnjs.cloudflare.com/ajax/libs/async/0.9.0/async.js"
                 ; "js/underscore.js"
                 "http://cdnjs.cloudflare.com/ajax/libs/underscore.js/1.6.0/underscore-min.js"
                 (format "https://maps.googleapis.com/maps/api/js?key=%s" google-maps-api-key)
                 "js/chart.js")]
    [:body
     [:div 
      {:style "position:relative;
              height:100%;" :class "result"}

      [:svg ]

      ; TODO why isn't error in the middle of the page?
      [:div
        {:class "center-transparent-in-relative error hidden"}
       [:div {:class "center-large"
              :style "color: #7C8479;
                     margin-left: auto;
                     margin-right: auto;" }
        "Hmm! We couldn't find your place!"]]

      [:div 
        {:class "center-transparent-in-relative loading"}
       [:div {:id "floatingCirclesG"
              :style "margin-left: auto;
                     margin-right: auto;"}
        (for [i (range 1 9)]
          [:div {:class "f_circleG"
                 :id (format "frotateG_%02d" i)}])]
       
       ; TODO put location in loading title
       ; TODO more interesting font ?
       [:div
        {:id "loading-label"
         :class "center-large"
         :style "color: #7C8479;"}
        "Searching"] ] ]]))

