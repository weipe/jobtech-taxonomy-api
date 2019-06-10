(ns jobtech-taxonomy-api.test.test-utils
  (:require [clojure.test :refer :all]
            [muuntaja.core :as m]
            [ring.mock.request :refer :all]
            [cprop.source :as source]
            [jobtech-taxonomy-api.handler :refer :all]
            [jobtech-taxonomy-api.config :refer :all]
            [jobtech-taxonomy-api.db.database-connection :refer :all]
            [jobtech-taxonomy-api.middleware.formats :as formats]
            [jobtech-taxonomy-api.middleware :as middleware]
            [jobtech-taxonomy-database.datomic-connection :as db]
            [datomic.client.api :as d]
            [mount.core :as mount]))

(def header-auth-user  { :key "api-key", :val (middleware/get-token :user)})
(def header-auth-admin { :key "api-key", :val (middleware/get-token :admin)})

(defmacro with-properties [property-map & body]
  "Run a badly simulated closure with a system property. Not thread safe."
  `(let [pm# ~property-map
         props# (into {} (for [[k# v#] pm#]
                           [k# (System/getProperty k#)]))]
     (doseq [k# (keys pm#)]
       (System/setProperty k# (get pm# k#)))
     (try
       ~@body
       (finally
         (doseq [k# (keys pm#)]
           (if-not (get props# k#)
             (System/clearProperty k#)
             (System/setProperty k# (get props# k#))))))))

(defn fixture [f]
  "Setup a temporary database, run (f), and then teardown the database."
  (letfn [(replace-db-name [c db-name]
            "Two tests cannot run simultanously, so generate unique names."
            (assoc c :datomic-name db-name))]
    (let [file-config (source/from-file
                       jobtech-taxonomy-api.config/integration-test-resource)
          db-name (str (get file-config :datomic-name) "-" (rand-int Integer/MAX_VALUE))
          config (replace-db-name file-config db-name)]
      (with-properties {"integration-test-db" db-name}

        (prn (db/list-databases config))

        (db/create-database config)

        ;; The purpose of loop below is to perform repeated attempts to
        ;; initialise the newly created database. It will succeed as soon as the
        ;; database engine is ready creating the database.
        (loop [acc 0]
          (cond
            (>= acc 10) (throw (Exception. "Database cannot be initialised"))
            :else (do
                    (if (= "DATABASE-DOWN"
                           (try
                             (db/init-new-db (db/get-conn config))
                             (Thread/sleep 100)
                             "DATABASE-UP"
                             (catch Exception e "DATABASE-DOWN")))
                      (recur (+ acc 1))
                      1))))

        (mount/start #'jobtech-taxonomy-api.db.database-connection/conn
                     #'jobtech-taxonomy-api.config/env
                     #'jobtech-taxonomy-api.handler/app)
        (f)

        (mount/stop #'jobtech-taxonomy-api.db.database-connection/conn
                    ;#'jobtech-taxonomy-api.config/env
                    ;#'jobtech-taxonomy-api.handler/app
                    )

        (db/delete-database config)
        ))))

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
