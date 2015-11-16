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


(ruleset
  ("basic"
    (:get "minimal"
      succeed)

    (:get "uid"
      :requires [uid]
      (== uid "tobi"))

    (:get "mail"
      :requires [uid mail]
      (== uid "tobi"))))

(t/deftest basic-test
  (t/testing "basic-minimal"
    (let [rule (get rules ["basic" :get "minimal"])]
      (t/is (= :dontknowyet
               (rule {})))))

  (t/testing "basic-uid"
    (let [rule (get rules ["basic" :get "uid"])]
      (t/is (= [["tobi"]]
               (rule {"uid" "tobi"})))))

  (t/testing "basic-mail"
    (let [rule (get rules ["basic" :get "uid"])]
      (t/is (= [["tobi"]]
               (rule {"uid" "tobi"}))))))