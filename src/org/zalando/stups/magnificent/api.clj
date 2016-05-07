(ns org.zalando.stups.magnificent.api
  (:require [org.zalando.stups.friboo.system.http :refer [def-http-component]]
            [org.zalando.stups.magnificent.util :as util]
            [ring.util.response :as ring]
            [org.zalando.stups.friboo.log :as log]
            [io.sarnowski.swagger1st.util.api :as api]
            [org.zalando.stups.magnificent.external.team :as team]
            [org.zalando.stups.magnificent.external.user :as user]
            [org.zalando.stups.magnificent.external.account :as account]
            [org.zalando.stups.friboo.ring :as fring]))

(def-http-component API "api/magnificent-api.yaml" [])
(def default-http-configuration {:http-port 8080})

(defn- find-user
  [realm user request]
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

(defn get-user
  [{:keys [realm user]} request]
  (->
    (find-user realm user request)
    ring/response
    fring/content-type-json))

(defn get-user-self
  [_ request]
  (let [realm (-> request
                (get-in [:tokeninfo "realm"])
                util/strip-leading-slash)
        uid   (get-in request [:tokeninfo "uid"])]
    (ring/redirect (str "/users/" realm "/" uid))))

(defn get-accounts
  [{:keys [type]} request]
  (->
    (account/get-accounts
      (get-in request [:configuration :account-api])
      type
      (get-in request [:tokeninfo "access_token"]))
    ring/response
    fring/content-type-json))

(defn get-account
  [{:keys [type account]} request]
  (->
    (account/get-account
      (get-in request [:configuration :account-api])
      type
      account
      (get-in request [:tokeninfo "access_token"]))
    ring/response
    fring/content-type-json))

(defn get-teams
  [_ request]
  (->
    (team/get-teams
      (get-in request [:configuration :team-api])
      (get-in request [:tokeninfo "access_token"]))
    ring/response
    fring/content-type-json))

(defn get-team
  [{:keys [team]} request]
  (let [team-api  (get-in request [:configuration :team-api])
        kio-api   (get-in request [:configuration :kio-api])
        token     (get-in request [:tokeninfo "access_token"])
        team-data (team/get-team
                    team-api
                    team
                    token)
        robots    (user/get-robot-users
                    kio-api
                    team
                    token)
        members   (apply conj (:members team-data) robots)]
    (->
      team-data
      (assoc :members members)
      ring/response
      fring/content-type-json)))


(defn get-auth
  [{:keys [authrequest]} request]
  (let [{:keys [team policy]} authrequest
        realm          (util/strip-leading-slash (get-in request [:tokeninfo "realm"]))
        account-api    (get-in request [:configuration :account-api])
        token          (get-in request [:tokeninfo "access_token"])
        user           (get-in request [:tokeninfo "uid"])
        member-id      (util/member-identifier {:id user :realm realm})
        allowed-realm? #{"employees" "services"}]
    ; just to be able to introduce more policies in the future
    (when-not (= "relaxed-radical-agility" policy)
      (api/throw-error 404 (str "Policy " policy " not found")))
    ; has to be an internal user
    (when-not (allowed-realm? realm)
      (api/throw-error 403 "Not an internal user"))
    ; if user is human and not a team member, check account access
    (let [team-data    (get-team {:team team} request)
          team-member? (set (map util/member-identifier (:members team-data)))]
      (when (not (team-member? member-id))
        (if (= realm "employees")
          ; TODO: sync calls :(
          (let [account-data    (doall (map
                                         #(account/get-account
                                           account-api
                                           (:type %)
                                           (:id %)
                                           token)
                                         (:accounts team-data)))
                account-member? (->> account-data
                                  (map :members)
                                  (apply conj)
                                  (map util/member-identifier)
                                  set)]
            (when-not (account-member? member-id)
              (api/throw-error 403 (str "Not a team member or access to its accounts: " member-id))))
          (api/throw-error 403 "Service user does not belong to team")))
      (ring/response "OK"))))
