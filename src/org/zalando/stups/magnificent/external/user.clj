(ns org.zalando.stups.magnificent.external.user
  (:require [clj-http.client :as http]
            [org.zalando.stups.friboo.ring :as util]
            [com.netflix.hystrix.core :refer [defcommand]]))

(defn get-user [user-api realm uid token])

