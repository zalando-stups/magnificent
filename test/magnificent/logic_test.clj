(ns magnificent.logic-test
  (:refer-clojure :exclude [==])
  (:require [clojure.test :as t]
            [clojure.core.logic :refer :all]
            [magnificent.logic :refer :all]))

(t/deftest get-opt-test
  (t/testing "without optional argument"
    (t/is (= [false [1 2]] (get-opt :nonexistant false [1 2]))))
  (t/testing "with optional argument"
    (t/is (= [:bar [1 2]] (get-opt :foo false [:foo :bar 1 2])))))


(policy
  (api "basic"
       (req "GET" "/minimal"
            succeed)))

(t/deftest basic-test
  (t/testing "basic-minimal"
    (let [decision (policy-fn {"http-api" "basic"
                               "http-method" "get"
                               "http-path-key" "/minimal"})]
      (t/is (= :dontknowyet decision)))))