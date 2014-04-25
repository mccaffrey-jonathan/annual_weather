(ns annual-weather.routes
  (:use compojure.core
        annual-weather.views
        [hiccup.middleware :only (wrap-base-url)])
  (:require [annual-weather.data-web :as data-web]
            [ring.middleware.json :as json]
            [ring.middleware.logger :as logger]
            [ring.middleware.reload :as reload]
            [ring.middleware.keyword-params :as keyword-params]
            [ring.util.codec :as rcodec]
            [ring.util.response :as rresponse]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]))

(defroutes page-routes
  (GET "/" [] (index-page))
  (route/resources "/" :root "public"))

(defroutes api-routes
  (-> (GET "/search" {{q :q} :params} [] 
           (-> q
               rcodec/url-decode
               data-web/search-handler
               rresponse/response))
           json/wrap-json-response))

(defroutes main-routes
  (handler/api api-routes)
  (handler/site page-routes))

(def reload-on-request
  '(annual-weather data data-web routes views))

(def app
  (-> main-routes
      (logger/wrap-with-logger)
      (reload/wrap-reload reload-on-request)
      (keyword-params/wrap-keyword-params)
      (wrap-base-url)))
