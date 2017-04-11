(ns org.zalando.stups.magnificent.external.team
  (:require [clj-http.client :as http]
            [slingshot.slingshot :refer [try+]]
            [org.zalando.stups.friboo.ring :as r]
            [org.zalando.stups.friboo.log :as log]
            [org.zalando.stups.magnificent.external.account :as account]
            [org.zalando.stups.magnificent.util :refer [defmemoized]]
            [com.netflix.hystrix.core :refer [defcommand]]
            [org.zalando.stups.magnificent.util :as util]))

(defn condense-team
  [team]
  {:id        (:id team)
   :full_name (:name team)})

(defn format-team
  [team]
  {:id        (:id team)
   :full_name (:name team)
   :mail      (:mail team)
   :members   (map
                #(assoc
                  {}
                  :id %
                  :realm "employees")
                (:member team))
   :accounts  (map
                #(select-keys % [:id :type])
                (:infrastructure-accounts team))
   :aliases (:alias team)})
(defcommand fetch-team
  [team-api team-id token]
  (->
    (http/get
      (r/conpath team-api "/teams/" team-id)
      {:oauth-token token
       :as          :json})
    :body
    format-team))

(defcommand fetch-teams
  [team-api token]
  (->>
    (http/get
      (r/conpath team-api "/teams")
      {:oauth-token token
       :as          :json})
    :body
    (map condense-team)))

(defcommand fetch-human-teams
  [team-api token user]
  (->>
    (http/get
      (r/conpath team-api "/teams")
      {:oauth-token  token
       :query-params {:member user}
       :as           :json})
    :body
    (map condense-team)))

(defcommand fetch-robot-teams
  "Strip robot prefix, go to Kio, get team, fetch team/account"
  [team-api account-api kio-api token user]
  (let [app-id             (util/strip-robot-prefix user)
        kio-resp           (http/get
                             (r/conpath kio-api "/apps/" app-id)
                             {:oauth-token token
                              :as          :json})
        team-or-account-id (get-in kio-resp [:body :team_id])]
    (try+
      (->
        (http/get
          (r/conpath team-api "/teams/" team-or-account-id)
          {:oauth-token token
           :as          :json})
        :body
        condense-team
        vector)
      (catch [:status 404] []
        ; not a team!?
        (try+
          (let [account-resp (account/get-account account-api "aws" team-or-account-id token)
                owning-team  (:owner account-resp)]
            (->
              (http/get
                (r/conpath team-api "/teams/" owning-team)
                {:oauth-token token
                 :as          :json})
              :body
              condense-team
              vector))
          (catch [:status 404] []
            []))))))

(defmemoized get-team fetch-team)
(defmemoized get-teams fetch-teams)
(defmemoized get-human-teams fetch-human-teams)
(defmemoized get-robot-teams fetch-robot-teams)
