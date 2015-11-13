(ns magnificent.core
  (:require [org.httpkit.server :as httpkit]
             [cheshire.core :as json]
             [clojure.java.io :as io])
  (:gen-class))

(defn authorize [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string request)})

(defn -main [& args]
  (httpkit/run-server
    (fn [request]
      (if (and (= :post (:request-method request))
               (= "/stups-api/authorize" (:uri request)))

        (-> request
            :body
            io/reader
            json/parse-stream
            authorize)

        {:status 404
         :headers {"Content-Type" "text/plain"}
         :body "unknown endpoint"}))

    {:port 8080})
  (println "Listening on :8080 for requests on POST /stups-api/authorize"))
