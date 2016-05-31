(ns org.zalando.stups.magnificent.external.team
  (:require [clj-http.client :as http]
            [slingshot.slingshot :refer [try+]]
            [org.zalando.stups.friboo.ring :as r]
            [org.zalando.stups.magnificent.util :refer [defmemoized]]
            [com.netflix.hystrix.core :refer [defcommand]]))

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
                (:infrastructure-accounts team))})

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

(defcommand fetch-team
  [team-api team-id token]
  (->
    (http/get
      (r/conpath team-api "/teams/" team-id)
      {:oauth-token token
       :as          :json})
    :body
    format-team))

(defmemoized get-team fetch-team)
(defmemoized get-teams fetch-teams)
(defmemoized get-human-teams fetch-human-teams)
