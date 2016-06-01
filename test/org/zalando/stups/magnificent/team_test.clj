(ns org.zalando.stups.magnificent.team-test
  (:require [clojure.test :refer :all]
            [clj-http.client :as http]
            [org.zalando.stups.magnificent.test-helper :as h]
            [org.zalando.stups.magnificent.external.team :as team]))

(defn as-http-resp
  [body & [code]]
  {:status (or code 200)
   :body   body})

;[team-api account-api kio-api token user]
(deftest get-robot-teams
  (testing "should work if team-id is a team"
    (with-redefs [http/get (h/sequentially
                             (as-http-resp {:team_id "torch"})
                             (as-http-resp {:id   "torch"
                                            :name "TORCH"}))]
      (let [robot-teams (team/fetch-robot-teams "team-api" "account-api" "kio-api" "token" "robot-user")]
        (is (= 1 (count robot-teams)))
        (is (= "torch" (-> robot-teams first :id))))))

  (testing "should work if team-id is an account"
    (with-redefs [http/get (h/sequentially-fns
                             (constantly (as-http-resp {:team_id "stups-test"}))
                             (h/throwing+ "Not found" {:status 404})
                             (constantly (as-http-resp {:owner "stups"}))
                             (constantly (as-http-resp {:id   "stups"
                                                        :name "STUPS"}))) s]
      (let [robot-teams (team/fetch-robot-teams "team-api" "account-api" "kio-api" "token" "robot-user")]
        (is (= 1 (count robot-teams)))
        (is (= "stups" (-> robot-teams first :id))))))

  (testing "should return empty vector if there no such account"
    (with-redefs [http/get (h/sequentially-fns
                             (constantly (as-http-resp {:team_id "foo"}))
                             (h/throwing+ "Not found" {:status 404}))]
      (let [robot-teams (team/fetch-robot-teams "team-api" "account-api" "kio-api" "token" "robot-user")]
        (is (empty? robot-teams)))))

  (testing "should return empty vector if there no owning team for an account"
    (with-redefs [http/get (h/sequentially-fns
                             (constantly (as-http-resp {:team_id "foo"}))
                             (h/throwing+ "Not found" {:status 404})
                             (constantly (as-http-resp {:owner "bar"}))
                             (h/throwing+ "Not found" {:status 404}))]
      (let [robot-teams (team/fetch-robot-teams "team-api" "account-api" "kio-api" "token" "robot-user")]
        (is (empty? robot-teams)))))

  (testing "it should throw if a non-404 error occors"
    (with-redefs [http/get (h/sequentially-fns
                             (constantly (as-http-resp {:team_id "stups"}))
                             (h/throwing+ "UAAAH" {:status 503}))]
      (is (thrown? Exception (team/fetch-robot-teams "team-api" "account-api" "kio-api" "token" "robot-user"))))))
