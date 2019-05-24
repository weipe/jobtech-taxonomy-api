(ns jobtech-taxonomy-api.db.database-connection
  (:require
   [datomic.client.api :as d]
   [mount.core :refer [defstate]]
   [jobtech-taxonomy-api.config :refer [env]]
   ))


(defn get-client [] (d/client (:datomic-cfg env)))

(defstate ^{:on-reload :noop} conn
  :start (d/connect (get-client)  {:db-name (:datomic-name env)}))

(defn get-db [] (d/db conn))

(defn get-conn "" []
  (d/connect (get-client)  {:db-name (:datomic-name env)}))
