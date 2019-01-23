(ns jobtech-taxonomy-api.db.core
  (:require

   [datomic.client.api :as d]
            [mount.core :refer [defstate]]
            [jobtech-taxonomy-api.config :refer [env]]

            ))

#_(defstate conn
  :start (-> env :database-url d/connect)
  :stop (-> conn .release))


(defn get-client [] (d/client (:datomic-cfg env)))



(defstate ^{:on-reload :noop} conn
  :start (d/connect (get-client)  {:db-name (:datomic-name env)} )
  )


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

(def show-term-history-query
  '[:find ?e ?aname ?v ?tx ?added
    :where
    [?e ?a ?v ?tx ?added]
    [?a :db/ident ?aname]])


(defn ^:private format-result [result-list]
  (->> result-list
       (sort-by first)
       (partition-by #(let [[entity col value tx is-added ] %] entity)) ; group by entity for better readability
       ;; The rest is for placing the result vectors into neat,
       ;; self-explanatory hashmaps - suitable for jsonifying later
       (map (fn [entity-group]
              (map (fn [entity]
                     (let [[ent col value tx is-added ] entity]
                       { :entity ent :col col :value value :tx tx :op (if is-added 'add 'retract ) }))
                   entity-group)))))


(defn ^:private show-term-history-back [q db]
  (->>
   (d/q q db)
   (format-result)))

(defn show-term-history []
  (show-term-history-back show-term-history-query (d/history (get-db))))

(def show-term-history-since-query
  '[:find ?e ?aname ?v ?tx ?added
    :in $ ?since
    :where
    [?e  ?a ?v ?tx ?added]
    [?a  :db/ident ?aname]
    [?tx :db/txInstant ?created-at]
    [(< ?since ?created-at)]])

(defn show-term-history-since [date-time]
  (->>
   (d/q show-term-history-since-query
       (get-db)
       date-time)
   (format-result)))


;; (d/q find-concept-by-preferred-term-query (get-db) "Ga")
