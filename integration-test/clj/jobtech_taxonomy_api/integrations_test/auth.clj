(ns jobtech-taxonomy-api.integrations-test.auth
  (:require [clojure.test :refer :all]
            [jobtech-taxonomy-api.handler :refer :all]
            [jobtech-taxonomy-api.db.core :refer :all]
            [jobtech-taxonomy-api.routes.services :as services]
            [jobtech-taxonomy-api.middleware :as middleware]
            [jobtech-taxonomy-api.integrations-test.test-utils :as util]
            [mount.core :as mount]))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'jobtech-taxonomy-api.db.core/conn
                 #'jobtech-taxonomy-api.config/env
                 #'jobtech-taxonomy-api.handler/app)
    (f)))

(deftest ^:integration unauthorized-access-public
  (testing "unauthorized access to /taxonomy/public-api/full-history"
    (let [[status body] (util/send-request-to-json-service :get "/taxonomy/public-api/full-history")]
      (is (and (= "unauthorized" (:error body))
               (= status 401))))))

(deftest ^:integration authorized-access-public
  (testing "unauthorized access to /taxonomy/public-api/full-history"
    (let [[status body] (util/send-request-to-json-service
                         :get "/taxonomy/public-api/full-history"
                         "api-key" (middleware/get-token :user))]
      (is (= status 200)))))

(deftest ^:integration unauthorized-access-private
  (testing "unauthorized access to /taxonomy/private-api/concept/types"
    (let [[status body] (util/send-request-to-json-service :get "/taxonomy/private-api/concept/types")]
      (is (and (= "unauthorized" (:error body))
               (= status 401))))))

(deftest ^:integration authorized-access-private
  (testing "authorized access to /taxonomy/private-api/concept/types"
    (let [[status body] (util/send-request-to-json-service :get "/taxonomy/private-api/concept/types"
                                                      "api-key" (middleware/get-token :admin))]
      (is (= status 200)))))

(deftest ^:integration authenticated-and-unauthorized-access-private
  (testing "authenticated and unauthorized access to /taxonomy/private-api/concept/types"
    (let [[status body] (util/send-request-to-json-service :get "/taxonomy/private-api/concept/types"
                                                      "api-key" (middleware/get-token :user))]
      (is (= status 401)))))
