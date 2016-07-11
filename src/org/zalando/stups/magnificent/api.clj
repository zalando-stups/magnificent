(ns org.zalando.stups.magnificent.api
  (:require [org.zalando.stups.friboo.system.http :refer [def-http-component]]
            [org.zalando.stups.magnificent.util :as util]
            [ring.util.response :as ring]
            [org.zalando.stups.friboo.log :as log]
            [clojure.core.async :as a :refer [put! chan <!! go]]
            [slingshot.slingshot :refer [try+]]
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
  (let [user-api-url (get-in request [:configuration :user-api])
        kio-api-url  (get-in request [:configuration :kio-api])
        token        (get-in request [:tokeninfo "access_token"])]
    (case realm
      "employees" (try+
                    (log/debug "Fetching user: %s" {:realm realm :user user})
                    (user/get-human-user
                      user-api-url
                      user
                      token)
                    (catch [:status 404] []
                      (log/warn "No such user: %s" {:realm realm :user user})
                      (api/throw-error 404 "No such user" {:realm realm :user user})))
      "services" (try+
                   (log/debug "Fetching user: %s" {:realm realm :user user})
                   (user/get-robot-user
                     kio-api-url
                     user
                     token)
                   (catch [:status 404] []
                     (log/warn "No such user: %s" {:realm realm :user user})
                     (api/throw-error 404 "No such user" {:realm realm :user user}))))))

(defn find-teams
  [realm user request]
  (let [team-api-url    (get-in request [:configuration :team-api])
        kio-api-url     (get-in request [:configuration :kio-api])
        account-api-url (get-in request [:configuration :account-api])
        token           (get-in request [:tokeninfo "access_token"])]
    (case realm
      "employees" (do
                    (log/debug "Fetching teams: %s" {:realm realm :user user})
                    (team/get-human-teams team-api-url token user))
      "services" (do
                   (log/debug "Fetching teams: %s" {:realm realm :user user})
                   (team/get-robot-teams team-api-url account-api-url kio-api-url token user)))))

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
    (log/debug "Fetching accounts: %s" {:user user :type type :realm "employees"})
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
    (try+
      (log/debug "Fetching account: %s" {:type type :account account})
      (account/get-account
        (get-in request [:configuration :account-api])
        type
        account
        (get-in request [:tokeninfo "access_token"]))
      (catch [:status 404] []
        (log/warn "No such account: %s" {:type type :account account})
        (api/throw-error 404 "No such account" {:type type :account account})))
    ring/response
    fring/content-type-json))

(defn get-teams
  [{:keys [user realm]} request]
  (let [teams (if (and user realm)
                (find-teams realm user request)
                (do
                  (log/debug "Fetching teams")
                  (team/get-teams
                    (get-in request [:configuration :team-api])
                    (get-in request [:tokeninfo "access_token"]))))]
    (->
      teams
      ring/response
      fring/content-type-json)))

(defn get-team
  [{:keys [team]} request]
  (let [team-api  (get-in request [:configuration :team-api])
        kio-api   (get-in request [:configuration :kio-api])
        token     (get-in request [:tokeninfo "access_token"])
        team-data (try+
                    (log/debug "Fetching team: %s" {:team team})
                    (team/get-team
                      team-api
                      team
                      token)
                    (catch [:status 404] []
                      (log/warn "No such team: %s" {:team team})
                      (api/throw-error 404 "No such team" {:team team})))
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

(defn get-team-or-account
  [team-or-account request]
  (try+
    (:body (get-team {:team team-or-account} request))
    (catch [:http-code 404] []
      (try+
        (let [account (:body (get-account {:type "aws" :account team-or-account} request))]
          (log/warn "Team is actually an account: %s" {:team-or-account team-or-account})
          account)
        (catch [:http-code 404] []
          (log/warn "No such team or account: %s" {:team-or-account team-or-account})
          {})))))

(defn post-auth
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
      (log/warn "Unknown policy: %s" {:policy policy})
      (api/throw-error 404 (str "Policy " policy " not found")))
    ; has to be an internal user
    (when-not (allowed-realm? realm)
      (log/warn "Invalid realm: %s" {:realm realm})
      (api/throw-error 403 "Not an internal user"))
    (let [team-data    (get-team-or-account team request)
          team-member? (->>
                         team-data
                         :members
                         (map util/member-identifier)
                         set)
          accounts     (get team-data :accounts [])]
      (if (team-member? member-id)
        (do
          (log/info "Access granted: %s" {:reason "Team member" :team team :member-id member-id})
          (ring/response "\"OK\""))
        (if (= realm "employees")
          ; if user is human and not a team member, check account access
          (let [accounts-channel (chan)
                nr-of-accounts   (count accounts)
                chan-seq!!       (fn chan-seq!! [ch]
                                   (when-let [v (<!! ch)] (cons v (chan-seq!! ch))))]
            (when (zero? nr-of-accounts)
              ; no accounts to look through
              (log/info "Access rejected: %s" {:reason "Not a team member and team has no accounts" :team team :member-id member-id})
              (api/throw-error 403 "Not a team member." {:member-id member-id :team team}))
            ; start fetching accounts in parallel
            (a/pipeline-blocking nr-of-accounts accounts-channel
                                 (map #(account/get-account account-api (:type %) (:id %) token))
                                 (a/to-chan accounts))
            ; turn channel to lazy seq, apply is-a-member? check, short circuit if the check passes
            (let [accounts-data (chan-seq!! accounts-channel)
                  is-a-member?  #(util/account-member? % member-id)]
              (when-not (some is-a-member? accounts-data)
                (log/info "Access rejected: %s" {:reason "No access to any team account" :team team :member-id member-id})
                (api/throw-error 403 "Not a team member or no access to its accounts." {:member-id member-id :team team})))
            (log/info "Access granted: %s" {:reason "Access to at least one team account" :member-id member-id :team team})
            (ring/response "\"OK\""))
          ; else just reject
          (api/throw-error 403 "Service user does not belong to team" {:member-id member-id :team team}))))))

(defn get-auth
  [params request]
  (post-auth params request))
