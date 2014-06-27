(ns annual-weather.routes
  (:use 
    clojure.tools.logging
    compojure.core
    annual-weather.views
    [hiccup.middleware :only (wrap-base-url)])
  (:require [annual-weather.data-web :as data-web]
            [clojure.walk :as walk]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.json :as json]
            [ring.middleware.logger :as logger]
            ; [ring.middleware.reload :as reload]
            [ring.middleware.keyword-params :as keyword-params]
            [ring.util.codec :as rcodec]
            [ring.util.response :as rresponse]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]))

(defroutes site-routes
  (GET "/site/search" [] (search-page))
  (GET "/site/chart" [] (chart-page))
  (route/resources "/res" :root "public")
  (route/resources "/" :root "public") )

(defroutes api-routes
  (-> (POST "/api/data" {params :params} [] 
            ring.util.response/not-found
           (-> params
               walk/keywordize-keys
               ; nil should return an automatic idiomatic 404
               ; data-web/nil-handler
               data-web/data-handler
               ))
      json/wrap-json-params
      json/wrap-json-response)
  (-> (GET "/api/search" {{q :q} :params} [] 
           (-> q
               rcodec/url-decode
               data-web/search-handler
               rresponse/response))
           json/wrap-json-response))

(defroutes main-routes
  (handler/api api-routes)
  (handler/site site-routes))

(def reload-on-request
  '(annual-weather app cdo data-web geocode routes utils views web-cache ))

(def app
  (-> main-routes
      (logger/wrap-with-logger)
      ; (reload/wrap-reload reload-on-request)
      (keyword-params/wrap-keyword-params)
      (wrap-base-url)))

(defn -main [port]
  (jetty/run-jetty app {:port (Integer. port) :join? false}))
