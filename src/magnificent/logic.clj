; implements all bridging logic between magnificent and core.logic
(ns magnificent.logic
  (:require [clojure.core.logic :as l]
            [magnificent.tools :as tools]))

(defn get-opt
  "Takes a keyword k and checks if its the first in the sequence s. If so, returns
   a tuple with the second element and the rest after the second. If k does not
   match, the tuple will contain d (default) and s."
  [k d s]
  (if (= k (first s))
    [(second s) (drop 2 s)]
    [d s]))

(defmacro policy [& body]
  (let [[with-token-resolver body] (get-opt :with-token-resolver tools/get-oauth2-tokeninfo body)
        [with-context body] (get-opt :with-context [] body)
        [with-context-resolvers body] (get-opt :with-context-resolvers {} body)
        ; construct list of all query arguments with the defaults and the custom ones
        lvars (-> [(symbol "http-api") (symbol "http-method") (symbol "http-path-key")]
                  (concat with-context)
                  (concat (keys with-context-resolvers)))]
    `(defn ~(symbol "policy-fn") [request#]
       (l/run* [~@lvars]
               (l/conde
                 ~@body)))))

(defmacro api [name & body]
  `[(l/== ~(symbol "http-api") ~name)
    (l/conde
      ~@body)])

(defmacro req [http-method http-path-key & body]
  `[(l/== ~(symbol "http-method") ~http-method)
    (l/== ~(symbol "http-path-key") ~http-path-key)
    ~@body])