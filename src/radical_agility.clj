(ns radical-agility
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer :all]
            [magnificent.logic :refer [ruleset]]
            [magnificent.tools]))

; 1. on request, the corresponding rule will be found
; 2. if a token was given, it gets resolved via JWT or tokeninfo
; 3. the resulting token information will be merged with the general context information
; 4. :require picks the defined information from the merged context and defines them as logic variables.
; 4a. for production, all variable will be bound via constraints to the actual context values
; 4b. for testing, the variables will not be bound and core.logic can show you possible evaluations
; 5. all :resolve keys will be resolved and added as fresh constraints as well

(ruleset

  :with-resolvers
    {:teams magnificent.tools/get-teams
     :accounts magnificent.tools/get-accounts})

  ("pierone"

    (:get "/ping"
      :require [team] :resolve [teams]
      (membero team teams))

    (:get "/ping2"
      :require [team scopes] :resolve [teams]
      (conde
        [(membero team teams)]
        [(membero "application.write" scopes)]))

    (:get "/ping3"
      :require [realm]
      (== "/services" realm))

    (:get "/ping4"
      :require [uid]))

