(ns jobtech-taxonomy-api.test.test-utils
  (:require [clojure.test :refer :all]
            [muuntaja.core :as m]
            [ring.mock.request :refer :all]
            [jobtech-taxonomy-api.handler :refer :all]
            [jobtech-taxonomy-api.config :refer :all]
            [jobtech-taxonomy-api.db.database-connection :refer :all]
            [jobtech-taxonomy-api.middleware.formats :as formats]
            [jobtech-taxonomy-api.middleware :as middleware]
            [mount.core :as mount]))

(def header-auth-user  { :key "api-key", :val (middleware/get-token :user)})
(def header-auth-admin { :key "api-key", :val (middleware/get-token :admin)})

(defn fixture [already-run f]
  (if @already-run
    (do
      (mount/start #'jobtech-taxonomy-api.db.database-connection/conn
                   #'jobtech-taxonomy-api.config/env
                   #'jobtech-taxonomy-api.handler/app)
      (reset! already-run false)))
  (f))

(defn parse-json [body]
  (m/decode formats/instance "application/json" body))

(defn make-req [req [first & rest]]
  (if (nil? first)
    req
    (make-req (header req (get first :key) (get first :val)) rest)))

(defn make-request [method endpoint & {:keys [headers query-params]}]
  (let [req (request method endpoint)
        req-w-headers (if headers
                        (make-req req headers)
                        req)]
    (if query-params
      (query-string req-w-headers
                    (clojure.string/join "&" (map #(clojure.string/join "=" (list (get % :key) (get % :val))) query-params)))
      req-w-headers)))

(defn send-request [method endpoint & {:keys [headers query-params]}]
  (let [req (make-request method endpoint :headers headers, :query-params query-params)]
    (app req)))

(defn send-request-to-json-service [method endpoint & {:keys [headers query-params]}]
  (let [response (send-request method endpoint :headers headers, :query-params query-params)
        status (:status response)
        body (parse-json (:body response))]
    (list status body)))

;;:;;(defn x [method endpoint & {:keys [header query-params]}]
;;:;;  query-params)
;;:;;  (let [{hdr :header, qp :query-params} rest]
;;:;;    hdr))
;;:(x :get "endpoint" :header 1, :query-params {:x 1} )
