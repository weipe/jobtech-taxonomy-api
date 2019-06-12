(ns jobtech-taxonomy-api.config
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [clojure.set :as set]
            [mount.core :refer [args defstate]]))

(def integration-test-resource "env/integration-test/resources/config.edn")



(defn make-config []
  (letfn [(replace-db-name [config name]
            (assoc config :datomic-name name))]
    (let [base-list [(args)
                     (source/from-system-props)
                     (source/from-env)]
          merged-conf (load-config :merge base-list)
          inttest-db (System/getProperty "integration-test-db")
          checked-test-list (if (nil? inttest-db)
                              base-list
			      [(replace-db-name merged-conf inttest-db)])]

    (load-config
     :merge
     checked-test-list))))

(defstate env
  :start
  (make-config))
