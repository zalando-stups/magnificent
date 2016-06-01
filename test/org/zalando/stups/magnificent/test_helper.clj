(ns org.zalando.stups.magnificent.test-helper
  (:require [slingshot.slingshot :refer [throw+]]))

(defn track
  "Returns a function that conjs its arguments into the atom"
  ([a action]
   (fn [& args]
     (let [prev (or (get @a action)
                  [])]
       (swap! a assoc action (conj prev (vec args)))))))

(defn throwing+
  "Returns a function that throws with the provided arguments when executed"
  [& [msg data]]
  (fn [& _]
    (throw+ (merge
              (or data {})
              {:message (or msg "any exception")}))))

(defn sequentially
  "Returns a function that returns provided arguments sequentially on every call"
  [& args]
  (let [calls (atom -1)
        limit (dec (count args))]
    (fn [& _]
      (if (neg? limit)
        nil
        (do
          (swap! calls inc)
          (nth args (if (> @calls limit)
                      limit
                      @calls)))))))

(defn sequentially-fns
  "Like sequentially, but takes functions which are executed to get the result to be returned"
  [& args]
  (let [calls (atom -1)
        limit (dec (count args))]
    (fn [& _]
      (if (neg? limit)
        nil
        (do
          (swap! calls inc)
          (let [fn (nth args (if
                               (> @calls limit)
                               limit
                               @calls))]

            (apply fn [])))))))
