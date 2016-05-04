(ns org.zalando.stups.magnificent.core
  (:require [org.zalando.stups.friboo.log :as log]
            [org.zalando.stups.friboo.config :as config]
            [org.zalando.stups.friboo.system :as system]
            [org.zalando.stups.magnificent.api :as api])
  (:gen-class))

(defn run
  [default-configuration]
  (let [configuration (config/load-configuration
                        (system/default-http-namespaces-and)
                        [api/default-http-configuration
                         default-configuration])
        system (system/http-system-map configuration api/map->API [])]
    (system/run configuration system)))

(defn -main
  [& _]
  (try
    (run {})
    (catch Exception e
      (log/error e "Could not start system because of %s." (str e))
      (System/exit 1))))
