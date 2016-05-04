(ns org.zalando.stups.magnificent.external.team
  (:require [clj-http.client :as http]
            [org.zalando.stups.friboo.ring :as util]
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
                #({:id % :realm "employees"})
                (:member team))
   :accounts  (map
                #(select-keys % [:id :type])
                (:infrastructure-accounts team))})

(defcommand get-teams
  [team-api token]
  (->>
    (http/get
      (util/conpath team-api "/teams")
      {:oauth-token token})
    :body
    (map condense-team)))

(defcommand get-team
  [team-api team-id token]
  (->>
    (http/get
      (util/conpath team-api "/teams/" team-id)
      {:oauth-token token})
    :body
    format-team))
