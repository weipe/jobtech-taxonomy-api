(ns jobtech-taxonomy-api.config
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [clojure.set :as set]
            [mount.core :refer [args defstate]]))

(def integration-test-resource "env/integration-test/resources/config.edn")

(defstate env
  :start
  (letfn [(replace-db-name [config name]
            (assoc config :datomic-name name))]
    (let [base-list [(args)
                     (source/from-system-props)
                     (source/from-env)]
          inttest-db (System/getProperty "integration-test-db")
          checked-test-list (if (nil? inttest-db)
                              base-list
                              [(replace-db-name (source/from-file integration-test-resource) inttest-db)])]
      ;; handy while debugging: dump current config to file for inspection
    (with-open [w (clojure.java.io/writer "/tmp/skrap.edn")]
      (binding [*out* w]
        (prn (load-config
              :merge
              checked-test-list))))

    (load-config
     :merge
     checked-test-list))))
