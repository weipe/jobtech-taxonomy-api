(ns jobtech-taxonomy-api.integrations-test.auth
  (:require [clojure.test :refer :all]
            [ring.mock.request :refer :all]
            [jobtech-taxonomy-api.handler :refer :all]
            [jobtech-taxonomy-api.db.core :refer :all]
            [jobtech-taxonomy-api.routes.services :as services]
            [jobtech-taxonomy-api.middleware :as middleware]
            [jobtech-taxonomy-api.middleware.formats :as formats]
            [muuntaja.core :as m]
            [mount.core :as mount]))

(defn parse-json [body]
  (m/decode formats/instance "application/json" body))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'jobtech-taxonomy-api.db.core/conn
                 #'jobtech-taxonomy-api.config/env
                 #'jobtech-taxonomy-api.handler/app)
    (f)))

(defn make-request [method endpoint & [hdr val]]
  (let [req (request method endpoint)]
    (if hdr
      (header req hdr val)
      req)))

(defn send-request [method endpoint & [hdr val]]
  (let [req (make-request :get endpoint hdr val)]
    (app req)))

(defn send-request-to-json-service [method endpoint & [hdr val]]
  (let [response (send-request method endpoint hdr val)
        status (:status response)
        body (parse-json (:body response))]
    (list status body)))

(deftest ^:integration unauthorized-access-public
  (testing "unauthorized access to /taxonomy/public-api/full-history"
    (let [[status body] (send-request-to-json-service :get "/taxonomy/public-api/full-history")]
      (is (and (= "unauthorized" (:error body))
               (= status 401))))))

(deftest ^:integration authorized-access-public
  (testing "unauthorized access to /taxonomy/public-api/full-history"
    (let [[status body] (send-request-to-json-service
                         :get "/taxonomy/public-api/full-history"
                         "api-key" (middleware/get-token :user))]
      (is (= status 200)))))

(deftest ^:integration unauthorized-access-private
  (testing "unauthorized access to /taxonomy/private-api/concept/types"
    (let [[status body] (send-request-to-json-service :get "/taxonomy/private-api/concept/types")]
      (is (and (= "unauthorized" (:error body))
               (= status 401))))))

(deftest ^:integration authorized-access-private
  (testing "unauthorized access to /taxonomy/private-api/concept/types"
    (let [[status body] (send-request-to-json-service :get "/taxonomy/private-api/concept/types"
                                                      "api-key" (middleware/get-token :admin))]
      (is (= status 200)))))
