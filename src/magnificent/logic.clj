; implements all bridging logic between magnificent and core.logic
(ns magnificent.logic
  (:require [clojure.core.logic :as l]))

(defn get-opt
  "Takes a keyword k and checks if its the first in the sequence s. If so, returns
   a tuple with the second element and the rest after the second. If k does not
   match, the tuple will contain d (default) and s."
  [k d s]
  (if (= k (first s))
    [(second s) (drop 2 s)]
    [d s]))

(defn process-rule
  "Generates a core.logic based function to evaluate all given constraints."
  [api http-method http-path-key constraints resolvers]
  (let [rule-key [api http-method http-path-key]
        [requires constraints] (get-opt :requires [] constraints)
        [resolves constraints] (get-opt :resolves [] constraints)
        ; TODO resolve resolves with resolvers
        ; TODO add resolves to requires
        ; TODO add == constraints to constraints for requires and resolves
        ]
    ; emit function that run* the rules later on
    [rule-key
     `(fn [request#]
        (l/run* ~(if (empty? requires) [(symbol "_")] [~@requires])
                ~@constraints))]))

(defmacro ruleset
  "Translates the configuration syntax to executable code. Will replace itself with a
   'rules' definition; a map with request-keys to rule function."
  [& body]
  (let [[resolvers apis] (get-opt :with-resolvers {} body)]
    `(def rules
       ~(into {}
          (apply concat
            ; process each api
            (for [[api & rules] apis]
                ; process each rule
                (for [[http-method http-path-key & constraints] rules]
                  (process-rule api http-method http-path-key constraints resolvers))))))))
