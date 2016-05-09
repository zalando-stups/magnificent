(ns org.zalando.stups.magnificent.util
  (:require [environ.core :refer [env]]
            [clojure.core.memoize :as memo]
            [clojure.core.cache :as cache]))

(defn strip-robot-prefix
  [uid & [prefix]]
  {:pre [(not (clojure.string/blank? uid))]}
  (let [prefix (or prefix (:robot-prefix env))]
    (if (.startsWith uid prefix)
      (.substring uid (.length prefix))
      uid)))

(defn add-robot-prefix
  [uid & [prefix]]
  {:pre [(not (clojure.string/blank? uid))]}
  (let [prefix (or prefix (:robot-prefix env))]
    (if (.startsWith uid prefix)
      uid
      (str prefix uid))))

(defn strip-leading-slash
  [realm]
  {:pre [(not (clojure.string/blank? realm))]}
  (if (.startsWith realm "/")
    (.substring realm 1)
    realm))

(defn member-identifier
  [user]
  (str (:realm user) "/" (:id user)))

(defn account-member?
  [account member-id]
  (let [member? (->> account
                  :members
                  (map member-identifier)
                  set)]
    (member? member-id)))

(defmacro defmemoized
  [name fn]
  `(def ~name
     (memo/fifo ~fn
       (cache/ttl-cache-factory {} :ttl 600000)
       :fifo/threshold 100)))
