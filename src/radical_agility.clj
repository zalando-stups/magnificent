(ns radical-agility
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer :all]
            [magnificent.logic :refer :all]
            [magnificent.tools]))

; default lvars are "http-api" "http-method" and "http-path-key", they are also automatically constraint in prod mode
; if a token was given, it will be resolved and merged with the context

; :with-context vars will be added as query parameters and also as constraints in prod mode
; :with-resolvers will use the clojure functions to retrieve static values which also get merged into context and
;                 added as constraint

; generates:
;   (defn policy-fn [request]
;     (run* [http-api http-method http-path-key team scopes teams accounts]
;       ...))

(policy
  :with-context [team scopes]

  :with-context-resolvers
  {teams    magnificent.tools/get-teams
   accounts magnificent.tools/get-accounts}

  (api "pierone"

       (req "GET" "/ping"
            (membero team teams))

       (req "GET" "/ping2"
            (conde
              [(membero team teams)]
              [(membero "application.write" scopes)]))

       (req "GET" "/ping3"
            (== "/services" realm))

       (req "GET" "/ping4"
            succeed)))