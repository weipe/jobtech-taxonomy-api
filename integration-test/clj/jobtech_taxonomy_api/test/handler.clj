(ns jobtech-taxonomy-api.test.handler
  (:require [clojure.test :refer :all]
            [ring.mock.request :refer :all]
            [jobtech-taxonomy-api.handler :refer :all]
            [jobtech-taxonomy-api.db.core :refer :all]
            [jobtech-taxonomy-api.middleware.formats :as formats]
            [muuntaja.core :as m]
            [mount.core :as mount]))


;; https://github.com/metosin/compojure-api/wiki/Testing-api-endpoints  ; skiljer sig lite


(comment
  För att köra testerna
  1. Byt till detta namespace
  2.  Kör (run-tests)   nu körs use-fixtures som laddar app
  3. nu kan du utveckla och köra testerna en och en)

(defn parse-json [body]
  (m/decode formats/instance "application/json" body))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'jobtech-taxonomy-api.db.core/conn
                 #'jobtech-taxonomy-api.config/env
                 #'jobtech-taxonomy-api.handler/app)
    (f)))

(deftest ^:integration test-app

  (testing "full history"
    (let [response (app (request :get "/taxonomy/public-api/full-history"))
          status (:status response)
          body (parse-json (:body response))
          an-event (first body)]
      (prn body)
      (is (= "CREATED" (:event-type an-event))))))

(defn get-preffered-term-from-get-api-concept [concept-id]
  (let [response (app (request :get "/taxonomy/private-api/concept" {"id" concept-id}))
        body (parse-json (:body response))
        term (:term/base-form (:concept/preferred-term (ffirst body)))]
    ;(prn body "   " term)
    term))

(defn test-get-preferred-term [concept-id expected-term]
  (let [actual-term (get-preffered-term-from-get-api-concept concept-id)]
    (is (= expected-term actual-term)
        (str "GET concept-id " concept-id " should have term " expected-term " not " actual-term))))

(deftest ^:integration test-call-api
  (testing "call api"
    (test-get-preferred-term "pdg3_Q49_97y" "Fartygsagent")
    (test-get-preferred-term "zYcH_Mn7_1hu"

                             "Skeppsklarerare/Waterclerk")))
