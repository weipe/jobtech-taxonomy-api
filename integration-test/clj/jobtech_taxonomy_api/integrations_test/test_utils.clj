(ns jobtech-taxonomy-api.integrations-test.test-utils
  (:require [clojure.test :refer :all]
            [muuntaja.core :as m]
            [ring.mock.request :refer :all]
            [jobtech-taxonomy-api.handler :refer :all]
            [jobtech-taxonomy-api.middleware.formats :as formats]
            [mount.core :as mount]))

(defn parse-json [body]
  (m/decode formats/instance "application/json" body))

(defn make-request [method endpoint & [hdr val]]
  (let [req (request method endpoint)]
    (if hdr
      (header req hdr val)
      req)))

(defn send-request [method endpoint & [hdr val]]
  (let [req (make-request method endpoint hdr val)]
    (app req)))

(defn send-request-to-json-service [method endpoint & [hdr val]]
  (let [response (send-request method endpoint hdr val)
        status (:status response)
        body (parse-json (:body response))]
    (list status body)))
