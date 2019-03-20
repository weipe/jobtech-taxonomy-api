(ns jobtech-taxonomy-api.test.handler
  (:require [clojure.test :refer :all]
            [ring.mock.request :refer :all]
            [jobtech-taxonomy-api.handler :refer :all]
            [jobtech-taxonomy-api.db.core :refer :all]
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

(deftest test-app

  #_(testing "main route"
    (let [response (app (request :get "/"))]
      (is (= 200 (:status response)))))

  #_(testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= 404 (:status response)))))


  (testing "full history"
    (let [response (app (request :get "/taxonomy/public-api/full-history"))]
      (prn response)
      (is (= 202 (:status response)))))

  )
