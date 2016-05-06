(ns org.zalando.stups.magnificent.external.user
  (:require [clj-http.client :as http]
            [org.zalando.stups.friboo.ring :as ring]
            [org.zalando.stups.magnificent.util :as util :refer [defmemoized]]
            [com.netflix.hystrix.core :refer [defcommand]]))

(defn format-human-user
  [user]
  {:id    (:login user)
   :name  (:name user)
   :email (:email user)
   :realm "employees"})

(defn format-robot-user
  [app]
  {:id    (util/add-robot-prefix (:id app))
   :realm "services"})

(defcommand fetch-robot-user
  [kio-api uid token]
  (->>
    (http/get
      (ring/conpath kio-api "/apps/" (util/strip-robot-prefix uid))
      {:oauth-token token})
    format-robot-user))

(defcommand fetch-human-user
  [user-api uid token]
  (->>
    (http/get
      (ring/conpath user-api "/employees/" uid)
      {:oauth-token token})
    format-human-user))

(defmemoized get-human-user fetch-human-user)
(defmemoized get-robot-user fetch-robot-user)
