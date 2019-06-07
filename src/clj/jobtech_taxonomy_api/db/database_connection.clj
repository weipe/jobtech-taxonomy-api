(ns jobtech-taxonomy-api.db.database-connection
  (:require
   [datomic.client.api :as d]
   ;;[datomic.api :as da]
   [mount.core :refer [defstate]]
   [jobtech-taxonomy-api.config :refer [env]]
   ))


(defn get-client [] (d/client (:datomic-cfg env)))

(defstate ^{:on-reload :noop} conn
  :start (do (println (str "start:conn " (:datomic-name env))) (d/connect (get-client)  {:db-name (:datomic-name env)}))
  ;;:stop (da/release (get-conn))
  )

(defn get-conn []
  (d/connect (get-client)  {:db-name (:datomic-name env)}))

;; This cannot use the conn var, as it will destroy the
;; integration tests (where the conn is made before the
;; tests fill the databases with test data).
(defn get-db [] (d/db (get-conn)))

(defn get-conn "" []
  (d/connect (get-client)  {:db-name (:datomic-name env)}))
