(ns org.zalando.stups.magnificent.external.account
  (:require [clj-http.client :as http]
            [org.zalando.stups.friboo.ring :as util]
            [com.netflix.hystrix.core :refer [defcommand]]))

(defn format-account [account]
  (->> account
    (select-keys [:id
                  :name
                  :disabled
                  :type
                  :members])
    (update-in [:members] #({:id    (:id %)
                             :realm "employees"}))))

(defcommand get-account
  [account-api type account-id token]
  (let [path (util/conpath account-api "/accounts/" type "/" account-id)]
    (->> (http/get path {:oauth-token token})
      :body
      format-account)))

(defcommand get-accounts
  [account-api type token]
  (let [path (util/conpath account-api "/accounts/" type)]
    (->> (http/get path {:oauth-token token})
      :body
      (map format-account))))
