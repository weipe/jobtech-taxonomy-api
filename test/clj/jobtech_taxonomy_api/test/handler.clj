(ns jobtech-taxonomy-api.test.handler
  (:require [clojure.test :refer :all]
            [ring.mock.request :refer :all]
            [jobtech-taxonomy-api.handler :refer :all]
            [jobtech-taxonomy-api.db.core :refer :all]
            [jobtech-taxonomy-api.middleware.formats :as formats]
            [muuntaja.core :as m]
            [mount.core :as mount]))


;; https://github.com/metosin/compojure-api/wiki/Testing-api-endpoints  ; skiljer sig lite


(defn parse-json [body]
  (m/decode formats/instance "application/json" body))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'jobtech-taxonomy-api.db.core/conn
                 #'jobtech-taxonomy-api.config/env
                 #'jobtech-taxonomy-api.handler/app)
    (f)))

(deftest test-app

  (testing "full history"
    (let [response (app  (request :get "/taxonomy/public-api/full-history"))
          status (:status response)
          body (parse-json (:body response))
          an-event (first body)]
      ;; (prn an-event)
      (is (= "CREATED" (:event-type an-event))))))
