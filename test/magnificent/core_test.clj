(ns magnificent.core-test
  (:refer-clojure :exclude [==])
  (:require [clojure.test :as t]
            [clojure.core.logic :refer :all]
            [magnificent.logic :refer :all]
            [magnificent.core :refer :all]
            [cheshire.core :as json]
            [ring.mock.request :as mock]))

(ruleset
  ("basic"
    (:get "ok"
      succeed)
    (:get "notok"
      fail)))

(t/deftest ring-test
  (t/testing "ok request"
    (let [req (-> (mock/request :post "/stups-api/authorize")
                (mock/body (json/generate-string
                             {:api "basic"
                              :http-method "get"
                              :http-path-key "ok"})))
          handler (ring-handler rules)
          response (handler req)]
      (t/is (= {:status 200} response))))

  (t/testing "not ok request"
    (let [req (-> (mock/request :post "/stups-api/authorize")
                  (mock/body (json/generate-string
                               {:api "basic"
                                :http-method "get"
                                :http-path-key "notok"})))
          handler (ring-handler rules)
          response (handler req)]
      (t/is (= {:status 200} response)))))