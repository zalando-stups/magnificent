(ns org.zalando.stups.magnificent.api
  (:require [org.zalando.stups.friboo.system.http :refer [def-http-component]]))

(def-http-component API "api/magnificent-api.yaml" [])
(def default-http-configuration {:http-port 8080})

(defn get-auth [])

(defn get-user [])

(defn get-user-self [])

(defn get-accounts [])

(defn get-account [])

(defn get-teams [])

(defn get-team [])

