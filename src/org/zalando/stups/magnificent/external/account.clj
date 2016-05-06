(ns org.zalando.stups.magnificent.external.account
  (:require [clj-http.client :as http]
            [org.zalando.stups.friboo.ring :as util]
            [org.zalando.stups.magnificent.util :refer [defmemoized]]
            [com.netflix.hystrix.core :refer [defcommand]]))

(defn format-account
  [account]
  (update-in
    (select-keys account
      [:id
       :name
       :disabled
       :type
       :members])
    [:members]
    (fn [members]
      (map
        #(assoc {}
          :id (:id %)
          :realm "employees")
        members))))

(defn condense-account
  [account]
  (select-keys account [:id :type]))

(defcommand fetch-account
  [account-api type account-id token]
  (->> (http/get
         (util/conpath account-api "/accounts/" type "/" account-id)
         {:oauth-token token
          :as          :json})
    :body
    format-account))

(defcommand fetch-accounts
  [account-api type token]
  (->> (http/get
         (util/conpath account-api "/accounts/" type)
         {:oauth-token token
          :as          :json})
    :body
    (map condense-account)))

(defmemoized get-account fetch-account)
(defmemoized get-accounts fetch-accounts)
