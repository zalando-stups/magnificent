; contains magnificent bootstrapping and request handling
(ns magnificent.core
  (:require [org.httpkit.server :as httpkit]
             [cheshire.core :as json]
             [clojure.java.io :as io])
  (:gen-class))

(defn authorize
  "Processes each request and returns an authorization decision."
  [request config]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string request)})

(defn load-config
  "Loads the ruleset with the given name and returns the initilized configuration object."
  [ruleset]
  (let [ruleset (symbol ruleset)]
    (require [ruleset])
    (symbol ruleset "config")))

(defn -main
  "The application first loads the given 'ruleset' and starts the HTTP endpoint afterwards."
  [& args]
  (let [ruleset (or (System/getenv "RULESET") "radical-agility")
        config (load-config ruleset)]
    (httpkit/run-server
      (fn [request]
        (if (and (= :post (:request-method request))
                 (= "/stups-api/authorize" (:uri request)))

          (-> request
              :body
              io/reader
              json/parse-stream
              (authorize config))

          {:status 404
           :headers {"Content-Type" "text/plain"}
           :body "unknown endpoint"}))

      {:port 8080})
    (println "Listening on :8080 for requests on POST /stups-api/authorize using ruleset \"" ruleset "\"")))
