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
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]
     [:title "Buentiempo!"] 
     [:link {:rel "stylesheet/less"
             :type "text/css"
             :href "/res/less/search.less"}]
     (include-css
       "http://netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css")
     (include-js
       "http://cdnjs.cloudflare.com/ajax/libs/jquery/2.1.1/jquery.min.js"
       "http://netdna.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js"
       ; TODO don't use client-side LESS in production
       "http://cdnjs.cloudflare.com/ajax/libs/less.js/1.6.3/less.min.js"
       "/res/js/search-page.js")
     ]
    [:body
     [:h1 {:class "main-title"
           :style "text-align: center;"}
      "Buentiempo!"]
     ; [:div {:class "pagination-center"}
     [:div {:class "site-wrapper"}
      [:div {:class "site-wrapper-inner"}
       [:div {:style "text-align: center;
                     vertical-align: middle;"}
        [:p "Enter your city, vacation destination, or any place below to chart its typical weather over each year" ]
        [:div {:class "container"}
        [:div {:class "row"}
         [:form {:class "form-horizontal" :role "form"}
         [:div {:class "form-group"}
;                :style "display: inline-block;"}
          [:div 
           {:class "col-md-6 col-md-offset-3"}
           [:div {:class "form-group"}
            [:label {:class "sr-only"
                     :for "place-search-bar"}
             "Address or place" ]
            [:input 
             {:type "search"
              :id "place-search-bar"
              :class "form-control"
              :placeholder "Address, or city and state" }]]]
          [:div 
           {:class "col-md-2"}
           [:button 
            {:type "button"
             :id "place-search-submit"
             :class "btn btn-block btn-primary"}
            "Go !"]]]]]]]]]
     ]))

(defn chart-page []
  (html5
    [:head

     [:meta {:http-equiv "Content-Type"
             :content "text/html;charset=utf-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]
     [:title "Buentiempo Chart !"] 
     [:link {:rel "stylesheet/less"
             :type "text/css"
             :href "/res/less/main.less"}]
     [:link {:rel "stylesheet/less"
             :media "print"
             :type "text/css"
             :href "/res/less/print.less"}]
     [:link {:rel "stylesheet/less"
             :media "screen"
             :type "text/css"
             :href "/res/less/screen.less"}]
     [:link {:rel "stylesheet/less"
             :type "text/css"
             :href "/res/less/spinner.less"}]
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
                 "/res/js/chart.js")]
    [:body
     [:div 
      {:style "position:relative;
              height:100%;" :class "result"}

      [:svg ]

      ; TODO why isn't error in the middle of the page?
      [:div
        {:class "center-transparent-in-relative error hidden"}
       [:div {:class "center-large error-heading"
              :style "margin-left: auto;
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
         :class "center-large loading-label" }
        "Searching..."] ] ]]))

