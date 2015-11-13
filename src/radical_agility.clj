(ns radical-agility
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer :all]
            [magnificent.logic :refer :all]
            [magnificent.tools :as tools]))

(defneeds
  teams tools/get-teams
  accounts tools/get-accounts)


(defapi "pierone"

  (defrule :get "/ping"
    :requires [team] :needs [teams]
    (membero team teams))

  (defrule :get "/ping2"
    :requires [team scopes] :needs [teams]
    (conde
      [(membero team teams)]
      [(membero "application.write" scopes)]))

  (defrule :get "/ping3"
    :requires [realm]
    (== "/services" realm))

  (defrule :get "/ping4"
    :requires [uid]))

