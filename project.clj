
(defproject annual-weather "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 ; Generic deps
                 [org.clojure/core.rrb-vector "0.0.10"]
                 [http-kit "2.1.16"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.4"] 
                 [prismatic/schema "0.2.0"]
                 [uncomplicate/fluokitten "0.3.0"]
                 [clj-time "0.6.0"]
                 [throttler "1.0.0"]
                 [environ "0.5.0"]
                 ; TODO better ns name, and export external deps
                 ; [clj-utils "LATEST"]
                 ;

                 ; Domain-Specific
                 [com.javadocmd/simplelatlng "1.3.0"]

                 ; Ring-Compojure
                 [compojure "1.1.1"]
                 [hiccup "1.0.0"]
                 [ring.middleware.logger "0.4.0"]
                 [ring/ring-json "0.2.0"]

                 ; DB
                 [com.novemberain/monger "2.0.0-rc1"]

                 ; Logging deps
                 [org.clojure/tools.logging "0.2.6"]
                 [org.slf4j/slf4j-log4j12 "1.7.1"]
                 [log4j/log4j "1.2.17"
                  :exclusions [javax.mail/mail
                               javax.jms/jms
                               com.sun.jmdk/jmxtools
                               com.sun.jmx/jmxri]]

                 ]

  :ring {:handler annual-weather.routes/app}
  :plugins [[lein-ring "0.7.1"]])
