(ns jobtech-taxonomy-api.db.events
  (:require
   [datomic.client.api :as d]
   [mount.core :refer [defstate]]))

(def show-concept-history
  '[:find ?e ?aname ?v ?tx ?added ?inst ?concept-id ?term ?pft
    :where
    [?e ?a ?v ?tx ?added]
    [?a :db/ident ?aname]
    [?e :concept/id ?concept-id]
    [?e :concept/preferred-term ?pft]
    [?pft :term/base-form ?term]
    [?tx :db/txInstant ?inst]])

(def show-concept-history-since-query
  '[:find ?e ?aname ?v ?tx ?added ?inst ?concept-id ?term ?pft
    :in $ ?since
    :where
    [?e ?a ?v ?tx ?added]
    [?a :db/ident ?aname]
    [?e :concept/id ?concept-id]
    [?e :concept/preferred-term ?pft]
    [?pft :term/base-form ?term]
    [?tx :db/txInstant ?inst]
    [(< ?since ?inst)]])

(def show-deprecated-replaced-by-query
  '[:find (pull ?c
                [:concept/id
                 :concept/description
                 {:concept/preferred-term [:term/base-form]}
                 {:concept/referring-terms [:term/base-form]}
                 {:concept/replaced-by [:concept/id
                                        {:concept/preferred-term [:term/base-form]}]}])

    ?inst
    :in $ ?since
    :where
    [?c :concept/deprecated true]
    [?c :concept/replaced-by ?rc ?tx]
    [?tx :db/txInstant ?inst]
    [(< ?since ?inst)]])

(defn get-deprecated-concepts-replaced-by-since [db date-time]
  (d/q show-deprecated-replaced-by-query db  date-time))

(defn  get-db-hist [db] (d/history db))

(defn group-by-transaction-and-entity [datoms]
  (group-by (juxt #(nth % 3) #(nth % 0)) datoms))

(defn group-by-attribute [grouped-datoms]
  (map #(group-by second %) grouped-datoms))

(defn filter-duplicate-preferred-term-datoms [[_ _ preferred-term-id _ operation _ _ preferred-term preferred-term-id-again]]
  (= preferred-term-id preferred-term-id-again))

(defn keep-after-update [[_ _ _ _ operation]]
  operation)

(defn is-event-update-preferred-term? [datoms-grouped-by-attribute]
  "checks if op is not all true or false"
  (if-let [datoms (:concept/preferred-term datoms-grouped-by-attribute)]
    (not (apply = (map #(nth % 4) datoms)))
    false))

(defn is-event-create-concept? [datoms-grouped-by-attribute]
  (if-let [datoms (:concept/id datoms-grouped-by-attribute)]
    (every? true? (map #(nth % 4) datoms))
    false))

(defn is-event-deprecated-concept? [datoms-grouped-by-attribute]
  (if-let [datoms (:concept/deprecated datoms-grouped-by-attribute)]
    (every? true? (map #(nth % 4) datoms))
    false))

(defn create-event-create-concept-from-datom [datoms-grouped-by-attribute]

  "TODO fix potential bugfest, first is a bit sketchy"
  (let [[_ _ _ transaction-id _ timestamp concept-id preferred-term]  (first (filter filter-duplicate-preferred-term-datoms (:concept/preferred-term datoms-grouped-by-attribute)))]
    {:event-type "CREATED"
     :transaction-id transaction-id
     :timestamp timestamp
     :concept-id concept-id
     :preferred-term preferred-term}))

(defn create-event-deprecated-concept-from-datom [datoms-grouped-by-attribute]
  (let [[_ _ _ transaction-id _ timestamp concept-id preferred-term]  (first (:concept/deprecated datoms-grouped-by-attribute))]
    {:event-type "DEPRECATED"
     :transaction-id transaction-id
     :timestamp timestamp
     :concept-id concept-id
     :preferred-term preferred-term
     :deprecated true}))

(defn create-event-updated-preferred-term [datoms-grouped-by-attribute]
  (let [datoms (filter filter-duplicate-preferred-term-datoms (:concept/preferred-term datoms-grouped-by-attribute))
        datom-before (filter #(not (keep-after-update %)) datoms)
        datom-after  (filter keep-after-update datoms)
        [[_ _ _ _ _ timestamp concept-id old-preferred-term]] datom-before
        [[_ _ _ transaction-id _ _ _ new-preferred-term]] datom-after]
    {:event-type "UPDATED"
     :transaction-id transaction-id
     :timestamp timestamp
     :concept-id concept-id
     :old-preferred-term old-preferred-term
     :new-preferred-term new-preferred-term}))

(defn determine-event-type [datoms-by-attibute]
  "This function will return nil events when the event is not CREATED, DEPRECATED or UPDATED.
Like replaced-by will return nil."
  (let [is-event-create-concept (is-event-create-concept? datoms-by-attibute)
        is-event-deprecated-concept (is-event-deprecated-concept? datoms-by-attibute)
        is-event-update-preferred-term (is-event-update-preferred-term? datoms-by-attibute)]

    (cond
      is-event-create-concept (create-event-create-concept-from-datom datoms-by-attibute)
      is-event-deprecated-concept (create-event-deprecated-concept-from-datom datoms-by-attibute)
      is-event-update-preferred-term (create-event-updated-preferred-term datoms-by-attibute))))

(defn convert-history-to-events [datoms]
  (let [grouped-datoms (map second (group-by-transaction-and-entity datoms))
        datoms-by-attibute (group-by-attribute grouped-datoms)
        events (filter some? (map determine-event-type datoms-by-attibute))]
    events))

(defn get-all-events [db]
  (sort-by :transaction-id
           (convert-history-to-events
            (d/q show-concept-history (get-db-hist db)))))

(defn get-all-events-since [db date-time]
  (sort-by :transaction-id
           (convert-history-to-events
            (d/q show-concept-history-since-query (get-db-hist db) date-time))))
