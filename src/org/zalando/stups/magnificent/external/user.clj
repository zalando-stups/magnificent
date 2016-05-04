(ns org.zalando.stups.magnificent.external.user
  (:require [clj-http.client :as http]
            [org.zalando.stups.friboo.ring :as util]
            [com.netflix.hystrix.core :refer [defcommand]]))

(defn format-human-user
  [user]
  ({:id    (:login user)
    :name  (:name user)
    :email (:email user)
    :realm "employees"}))

(defn format-robot-user
  [user]
  (throw "Not yet implemented"))

(defn get-formatter
  [realm]
  (if (= realm "employees")
    format-human-user
    (if (= realm "services")
      format-robot-user
      (throw "Invalid realm"))))

(defcommand get-user
  [user-api realm uid token]
  (let [formatter (get-formatter realm)]
    (->>
      (http/get
        (util/conpath user-api "/" realm "/" uid)
        {:oauth-token token})
      formatter)))
