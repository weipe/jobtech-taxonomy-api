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


(def get-database-instance-from-version-query
  '[:find ?txid
    :in $ ?version
    :where
    [?t :taxonomy-version/id ?version ?txid]
    ]
  )

(defn get-transaction-id-from-version [version]
  (ffirst (d/q get-database-instance-from-version-query (d/db (get-conn))  version))
  )

;; This cannot use the conn var, as it will destroy the
;; integration tests (where the conn is made before the
;; tests fill the databases with test data).
(defn get-db
  ([]
   (d/db (get-conn)))
  ([version]
   (if version
     (d/as-of (d/db (get-conn)) (get-transaction-id-from-version version))
     (get-db)
     )
   )
  )

(defn get-conn "" []
  (d/connect (get-client)  {:db-name (:datomic-name env)}))
