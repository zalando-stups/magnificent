; contains magnificent bootstrapping and request handling
(ns magnificent.core
  (:require [org.httpkit.server :as httpkit]
            [cheshire.core :as json]
            [clojure.java.io :as io])
  (:gen-class))

(defn json-response [status message & {:keys [payload]}]
  {:status  status
   :headers {"Content-Type" "application/json"}
   :body    (json/generate-string
              {:message message
               :payload payload})})

(defn authorize
  "Processes each request and returns an authorization decision."
  [request policy-fn]
  (try
    (policy-fn request)
    (catch Exception e
      (.printStackTrace e)
      (json-response 500 "Internal Server Error"))))

(defn load-policy-fn
  "Loads the ruleset with the given name and returns the initilized configuration object."
  [ruleset]
  (let [ruleset (symbol ruleset)]
    (require [ruleset])
    (symbol ruleset "policy-fn")))

(defn ring-handler
  "Routes requests to this http endpoint to its correct implementation."
  [rules]
  (fn [request]
    (if (and (= :post (:request-method request))
             (= "/stups-api/authorize" (:uri request)))

      (-> request
          :body
          io/reader
          json/parse-stream
          (authorize rules))

      (json-response 404 "Unknown endpoint."))))

(defn -main
  "The application first loads the given $POLICY and starts the HTTP endpoint afterwards."
  [& args]
  (let [policy-name (or (System/getenv "POLICY") "radical-agility")
        policy-fn (load-policy-fn policy-name)]
    (httpkit/run-server
      (ring-handler policy-fn)
      {:port 8080})
    (println "Listening on :8080 for requests on POST /stups-api/authorize using policy \"" policy-name "\"")))
