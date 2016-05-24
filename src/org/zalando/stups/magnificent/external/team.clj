(ns org.zalando.stups.magnificent.external.team
  (:require [clj-http.client :as http]
            [org.zalando.stups.friboo.ring :as util]
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
  [team-api token & [user]]
  (->>
    (http/get
      (util/conpath team-api "/teams")
      {:oauth-token token
       :query-params (when user
                       {"member" user})
       :as          :json})
    :body
    (map condense-team)))

(defcommand fetch-team
  [team-api team-id token]
  (->
    (http/get
      (util/conpath team-api "/teams/" team-id)
      {:oauth-token token
       :as          :json})
    :body
    format-team))

(defmemoized get-team fetch-team)
(defmemoized get-teams fetch-teams)
