(ns annual-weather.views
  (:use [hiccup core page]))

(defn index-page []
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
                 "js/main.js")]
    [:body
     [:div 
      {:style "position:relative;
              height:100%;" :class "result"}
      [:svg ]
      [:div 
       {:style "position:absolute; 
               top:0; 
               left:0; 
               height:100%;
               width:100%;
               background-color:rgba(0,0,0,0.0);"
        :class "loading"}
       [:div {:id "floatingCirclesG"
              :style "margin-left: auto;
                     margin-right: auto;"}
        (for [i (range 1 9)]
          [:div {:class "f_circleG"
                 :id (format "frotateG_%02d" i)}])]
       
       ; TODO put location in loading title
       ; TODO more interesting font ?
       [:div
        {:style "text-align: center;
                color: #7C8479;
                font-size: xx-large;
                font-family: sans-serif;"}
        "Loading YOURTOWN"] ] ]]))

