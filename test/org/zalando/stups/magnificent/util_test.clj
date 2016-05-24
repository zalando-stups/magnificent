(ns org.zalando.stups.magnificent.util-test
  (:require [clojure.test :refer :all]
            [org.zalando.stups.magnificent.util :as u]))

(deftest strip-leading-slash
  (testing "should strip exactly one leading slash"
    (are [in out] (= (u/strip-leading-slash in) out)
         "/employees" "employees"
         "//employees" "/employees"
         "employees" "employees")))

(deftest add-robot-prefix
  (testing "should add the prefix if not exists"
    (are [in out] (= (u/add-robot-prefix in "robot_") out)
         "foo" "robot_foo"
         "robot_foo" "robot_foo"
         "robofoo" "robot_robofoo")))

(deftest strip-robot-prefix
  (testing "should strip the prefix if exists"
    (are [in out] (= (u/strip-robot-prefix in "robot_") out)
         "robot_foo" "foo"
         "foo" "foo")))
