; implements all bridging logic between magnificent and core.logic
(ns magnificent.logic
  (:require [clojure.core.logic :as l]))

(defn- get-opt
  "Takes a keyword k and checks if its the first in the sequence s. If so, returns
   a tuple with the second element and the rest after the second. If k does not
   match, the tuple will contain d (default) and s."
  [k d s]
  (if (= k (first s))
    [(second s) (drop 2 s)]
    [nil s]))

(defmacro defrules
  "Translates the configuration syntax to executable code."
  [api & s]
  (let [[resolvers apis] (get-opt :with-resolvers {} s)]
    ; process each api
    (for [[api & rules] apis]
      ; process each rule
      (for [[http-method http-path-key & constraints] rules]
        (let [[requires constraints] (get-opt :requires [] constraints)
              [resolves constraints] (get-opt :resolves [] constraints)
              ; TODO resolve resolves with resolvers
              ; TODO add resolves to requires
              ; TODO add == constraints to constraints for requires and resolves
              ]
          ; emit function that run* the rules later on
          [[http-method http-path-key]
           `(fn [request]
              (run* [~@requires]
                    ~@constraints))])))))
