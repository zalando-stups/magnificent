(ns org.zalando.stups.magnificent.api
  (:require [org.zalando.stups.friboo.system.http :refer [def-http-component]]
            [org.zalando.stups.magnificent.util :as util]
            [ring.util.response :as ring]
            [org.zalando.stups.friboo.log :as log]
            [clojure.core.async :refer [put! chan <!! go]]
            [io.sarnowski.swagger1st.util.api :as api]
            [org.zalando.stups.magnificent.external.team :as team]
            [org.zalando.stups.magnificent.external.user :as user]
            [org.zalando.stups.magnificent.external.account :as account]
            [org.zalando.stups.friboo.ring :as fring]
            [io.sarnowski.swagger1st.util.api :as api]))

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
  [{:keys [type user]} request]
  (->
    (account/get-accounts
      (get-in request [:configuration :account-api])
      type
      (get-in request [:tokeninfo "access_token"])
      user)
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
  [{:keys [user]} request]
  (->
    (team/get-teams
      (get-in request [:configuration :team-api])
      (get-in request [:tokeninfo "access_token"])
      user)
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
  (let [{:keys [payload policy]} authrequest
        team           (:team payload)
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
    (let [team-data    (:body (get-team {:team team} request))
          team-member? (->>
                         team-data
                         :members
                         (map util/member-identifier)
                         set)
          accounts     (:accounts team-data)]
      (if (team-member? member-id)
        (ring/response "\"OK\"")
        (if (= realm "employees")
          ; if user is human and not a team member, check account access
          (let [channel        (chan)
                nr-of-accounts (count accounts)]
            (when (zero? nr-of-accounts)
              ; no accounts to look through
              (api/throw-error 403 (str "Not a team member: " member-id)))
            (doseq [account accounts]
              (go
                (put! channel (account/get-account
                                account-api
                                (:type account)
                                (:id account)
                                token))))
            (loop [account-data (<!! channel)
                   counter      1]
              (let [is-member? (util/account-member? account-data member-id)]
                ; throw when we're at the last account already and user is not a member
                (when (and (= counter nr-of-accounts)
                        (not is-member?))
                  (api/throw-error 403 (str "Not a team member or no access to its accounts: " member-id)))
                ; recur when there is at least one more account to look at
                (when (< counter nr-of-accounts)
                  (recur
                    (<!! channel)
                    (inc counter)))))
            (ring/response "\"OK\""))
          ; else just reject
          (api/throw-error 403 "Service user does not belong to team"))))))
