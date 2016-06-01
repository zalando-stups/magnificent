(ns org.zalando.stups.magnificent.api-test
  (:require [clojure.test :refer :all]
            [org.zalando.stups.magnificent.test-helper :as h]
            [org.zalando.stups.magnificent.external.team :as team]
            [org.zalando.stups.magnificent.external.user :as user]
            [org.zalando.stups.magnificent.external.account :as account]
            [org.zalando.stups.magnificent.api :as api]))

(def default-auth-params {:authrequest {:policy  "relaxed-radical-agility"
                                        :payload {:team "stups"}}})

(def default-request
  {:configuration {:user-api    "user-api"
                   :kio-api     "kio-api"
                   :team-api    "team-api"
                   :account-api "account-api"}
   :tokeninfo     {"realm"        "employees"
                   "uid"          "hermann"
                   "access_token" "token"}})

(deftest get-team
  (testing "should include service users and employees"
    (let [calls   (atom {})
          params  {:team "stups"}
          request default-request]
      (with-redefs [user/get-robot-users (comp
                                           (constantly [{:id    "robot_hal9000"
                                                         :realm "services"}])
                                           (h/track calls :robots))
                    team/get-team (comp
                                    (constantly {:members [{:id    "hermann"
                                                            :realm "employees"}]})
                                    (h/track calls :team))]
        (let [response    (api/get-team params request)
              robot-calls (:robots @calls)
              team-calls  (:team @calls)
              member?     (set (map :id (get-in response [:body :members])))]
          ; check mocked functions
          (is (= 1 (count robot-calls)))
          (is (= 1 (count team-calls)))
          (is (= (first robot-calls) ["kio-api" "stups" "token"]))
          (is (= (first team-calls) ["team-api" "stups" "token"]))
          ; check response
          (is (= 200 (:status response)))
          (is (member? "robot_hal9000"))
          (is (member? "hermann")))))))

(deftest get-auth
  (testing "it should take only one policy"
    (try
      (api/get-auth {:authrequest {:policy "foo"}} default-request)
      (is false)
      (catch Exception e
        (is (= (:http-code (ex-data e)) 404))
        (is (= (:message (ex-data e)) "Policy foo not found")))))

  (testing "it should not answer to requests by externals"
    (try
      (api/get-auth default-auth-params (assoc-in default-request [:tokeninfo "realm"] "customer"))
      (is false)
      (catch Exception e
        (is (= (:http-code (ex-data e)) 403))
        (is (= (:message (ex-data e)) "Not an internal user")))))

  (testing "it should accept team members"
    (let [calls (atom {})]
      (with-redefs [api/get-team (comp
                                   (constantly {:body {:members [{:id    "hermann"
                                                                  :realm "employees"}]}})
                                   (h/track calls :get-team))]
        (let [response (api/get-auth default-auth-params default-request)]
          (is (= 1 (count (:get-team @calls))))
          (is (= 200 (:status response)))))))

  (testing "it should not accept foreign robot users"
    (let [calls (atom {})]
      (with-redefs [api/get-team (comp
                                   (constantly {:body {:members [{:id    "hermann"
                                                                  :realm "employees"}]}})
                                   (h/track calls :get-team))]
        (let [request (assoc default-request :tokeninfo {"realm"        "services"
                                                         "uid"          "robot_hal9000"
                                                         "access_token" "token2"})]
          (try
            (api/get-auth default-auth-params request)
            (is false)
            (catch Exception e
              (is (= 1 (count (:get-team @calls))))
              (is (= (:http-code (ex-data e)) 403))
              (is (= (:message (ex-data e)) "Service user does not belong to team"))))))))

  (testing "it should accept employees with account access"
    (let [calls (atom {})]
      (with-redefs [api/get-team (comp
                                   (constantly {:body {:members  [{:id    "guenther"
                                                                   :realm "employees"}]
                                                       :accounts [{:type "aws"
                                                                   :id   "1337"}]}})
                                   (h/track calls :get-team))
                    account/get-account (comp
                                          (constantly {:members [{:id    "hermann"
                                                                  :realm "employees"}]})
                                          (h/track calls :get-account))]
        (let [response (api/get-auth default-auth-params default-request)]
          (is (= 200 (:status response)))
          (is (= 1 (count (:get-team @calls))))
          (is (= 1 (count (:get-account @calls))))))))

  (testing "it should not accept employees without account access"
    (let [calls (atom {})]
      (with-redefs [api/get-team (comp
                                   (constantly {:body {:members  [{:id    "guenther"
                                                                   :realm "employees"}]
                                                       :accounts [{:type "aws"
                                                                   :id   "1337"}]}})
                                   (h/track calls :get-team))
                    account/get-account (comp
                                          (constantly {:members [{:id    "rolf"
                                                                  :realm "employees"}]})
                                          (h/track calls :get-account))]
        (try
          (api/get-auth default-auth-params default-request)
          (is false)
          (catch Exception e
            (is (= (:http-code (ex-data e)) 403))
            (is (= 1 (count (:get-team @calls))))
            (is (= 1 (count (:get-account @calls))))))))))
