(ns jobtech-taxonomy-api.db.core
  (:require
            [datomic.api :as d]
            [mount.core :refer [defstate]]
            [jobtech-taxonomy-api.config :refer [env]]

            ))

(defstate conn
  :start (-> env :database-url d/connect)
  :stop (-> conn .release))


(defn get-db [] (d/db conn))


(def find-concept-by-preferred-term-query
  '[:find (pull ?c
                [
                 :concept/id
                 :concept/description
                 {:concept/preferred-term [:term/base-form]}
                 {:concept/referring-terms [:term/base-form]}])
    :in $ ?term
    :where [?t :term/base-form ?term]
    [?c :concept/preferred-term ?t]

    ])

(defn find-concept-by-preferred-term [term]
  (d/q find-concept-by-preferred-term-query (get-db) term)
  )

;; (d/q find-concept-by-preferred-term-query (get-db) "Ga")
