(ns jobtech-taxonomy-api.test.auth-test
  (:require [clojure.test :as test]
            [jobtech-taxonomy-api.handler :refer :all]
            [jobtech-taxonomy-api.db.core :refer :all]
            [jobtech-taxonomy-api.routes.services :as services]
            [jobtech-taxonomy-api.middleware :as middleware]
            [jobtech-taxonomy-api.test.test-utils :as util]
            [mount.core :as mount]))


(def run-fixture? (atom true))

(test/use-fixtures
  :each
  (fn [f]
    (if @run-fixture?
      (do
        (mount/start #'jobtech-taxonomy-api.db.core/conn
                     #'jobtech-taxonomy-api.config/env
                     #'jobtech-taxonomy-api.handler/app)
        (reset! run-fixture? false)))
    (f)))

(test/deftest ^:integration unauthorized-access-public
  (test/testing "unauthorized access to /v0/taxonomy/public/concept/types"
    (let [[status body] (util/send-request-to-json-service :get "/v0/taxonomy/public/concept/types")]
      (test/is (and (= "unauthorized" (:error body))
               (= status 401))))))

(test/deftest ^:integration authorized-access-public
  (test/testing "unauthorized access to /v0/taxonomy/public/concept/types"
    (let [[status body] (util/send-request-to-json-service
                         :get "/v0/taxonomy/public/concept/types"
                         "api-key" (middleware/get-token :user))]
      (test/is (= status 200)))))

(test/deftest ^:integration unauthorized-access-private
  (test/testing "unauthorized access to /v0/taxonomy/private/relation/types"
    (let [[status body] (util/send-request-to-json-service :get "/v0/taxonomy/private/relation/types")]
      (test/is (and (= "unauthorized" (:error body))
               (= status 401))))))

(test/deftest ^:integration authorized-access-private
  (test/testing "authorized access to /v0/taxonomy/private/relation/types"
    (let [[status body] (util/send-request-to-json-service :get "/v0/taxonomy/private/relation/types"
                                                      "api-key" (middleware/get-token :admin))]
      (test/is (= status 200)))))

(test/deftest ^:integration authenticated-and-unauthorized-access-private
  (test/testing "authenticated and unauthorized access to /v0/taxonomy/private/relation/types"
    (let [[status body] (util/send-request-to-json-service :get "/v0/taxonomy/private/relation/types"
                                                      "api-key" (middleware/get-token :user))]
      (test/is (= status 401)))))
