(ns org.zalando.stups.magnificent.external.team
  (:require [clj-http.client :as http]
            [org.zalando.stups.friboo.ring :as util]
            [com.netflix.hystrix.core :refer [defcommand]]))

(defn get-teams [team-api token])

(defn get-team [team-api team-id token])
