(ns org.zalando.stups.magnificent.api
  (:require [org.zalando.stups.friboo.system.http :refer [def-http-component]]
            [org.zalando.stups.magnificent.util :as util]
            [ring.util.response :refer [redirect]]
            [org.zalando.stups.magnificent.external.team :as team]
            [org.zalando.stups.magnificent.external.user :as user]
            [org.zalando.stups.magnificent.external.account :as account]))

(def-http-component API "api/magnificent-api.yaml" [])
(def default-http-configuration {:http-port 8080})

(defn get-auth
  [_ _]
  (throw "foo"))

(defn get-user
  [{:keys [realm user]} request]
  (let [user-api (get-in request [:configuration :user-api])
        kio-api  (get-in request [:configuration :kio-api])
        token    (get-in request [:tokeninfo "access_token"])]
    (if (= realm "employees")
      (user/get-human-user
        user-api
        user
        token)
      (if (= realm "services")
        (user/get-robot-user
          kio-api
          user
          token)))))

(defn get-user-self
  [_ request]
  (let [realm (-> request
                (get-in [:tokeninfo "realm"])
                util/strip-leading-slash)
        uid (get-in request [:tokeninfo "uid"])]
    (redirect (str "/user/" realm "/" uid))))

(defn get-accounts
  [_ request]
  (account/get-accounts
    (get-in request [:configuration :account-api])
    (get-in request [:tokeninfo "access_token"])))

(defn get-account
  [params request]
  (account/get-account
    (get-in request [:configuration :account-api])
    (:account params)
    (get-in request [:tokeninfo "access_token"])))

(defn get-teams
  [_ request]
  (team/get-teams
    (get-in request [:configuration :team-api])
    (get-in request [:tokeninfo "access_token"])))

(defn get-team
  [params request]
  (team/get-team
    (get-in request [:configuration :team-api])
    (:team params)
    (get-in request [:tokeninfo "access_token"])))

