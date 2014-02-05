(ns annual-weather.routes
  (:use compojure.core
        annual-weather.views
        [hiccup.middleware :only (wrap-base-url)])
  (:require 
    [annual-weather.data-scratchpad :as weather]
    [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [ring.middleware.json :as json]
            ))

(defroutes api-routes
  (GET "/climate" [] (weather/json-weather-data)
       ))

(defroutes main-routes
  (GET "/" [] (index-page))
  (handler/api api-routes)
  (route/resources "/")
  (route/not-found "Page not found"))

(def app
  (-> (handler/site main-routes)
      (wrap-base-url)))
