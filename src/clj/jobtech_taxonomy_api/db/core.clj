(ns jobtech-taxonomy-api.db.core
  (:require
   [datomic.client.api :as d]
   [mount.core :refer [defstate]]
   [jobtech-taxonomy-api.config :refer [env]]))

#_(defstate conn
    :start (-> env :database-url d/connect)
    :stop (-> conn .release))

(defn get-client [] (d/client (:datomic-cfg env)))

(defstate ^{:on-reload :noop} conn
  :start (d/connect (get-client)  {:db-name (:datomic-name env)}))

(defn get-db [] (d/db conn))

(def find-concept-by-preferred-term-query
  '[:find (pull ?c
                [:concept/id
                 :concept/description
                 {:concept/preferred-term [:term/base-form]}
                 {:concept/referring-terms [:term/base-form]}])
    :in $ ?term
    :where [?t :term/base-form ?term]
    [?c :concept/preferred-term ?t]])

(defn find-concept-by-preferred-term [term]
  (d/q find-concept-by-preferred-term-query (get-db) term))

(def find-concept-by-id-query
  '[:find (pull ?c
                [:concept/id
                 :concept/description
                 {:concept/preferred-term [:term/base-form]}
                 {:concept/referring-terms [:term/base-form]}])
    :in $ ?id
    :where [?c :concept/id ?id]])

(defn find-concept-by-id [id]
  (d/q find-concept-by-id-query (get-db) id))

(def get-concepts-for-type-query
  '[:find (pull ?c
                [:concept/id
                 :concept/description
                 :concept/category
                 {:concept/preferred-term [:term/base-form]}
                 {:concept/referring-terms [:term/base-form]}])
    :in $ ?type
    :where [?c :concept/category ?type]])

(defn get-concepts-for-type [type]
  (d/q get-concepts-for-type-query (get-db) type))

(def get-all-taxonomy-types-query
  ;;'[:find ?type :where [?e ?a ?v ?tx] [?a :concept/category ?type]])
  '[:find ?v :where [_ :concept/category ?v]])

(defn get-all-taxonomy-types "" []
  (->> (d/q get-all-taxonomy-types-query (get-db))
       (sort-by first)))

(def show-term-history-query
  '[:find ?e ?aname ?v ?tx ?added
    :where
    [?e ?a ?v ?tx ?added]
    [?a :db/ident ?aname]])

(defn ^:private format-result [result-list]
  (->> result-list
       (sort-by first)
       (partition-by #(let [[entity col value tx is-added] %] entity)) ; group by entity for better readability
       ;; The rest is for placing the result vectors into neat,
       ;; self-explanatory hashmaps - suitable for jsonifying later
       (map (fn [entity-group]
              (map (fn [entity]
                     (let [[ent col value tx is-added] entity]
                       {:entity ent :col col :value value :tx tx :op (if is-added 'add 'retract)}))
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; DEBUG TOOLS ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn stupid-debug
  "The env/dev/resources/config.edn is not read when REPLing here, so
  we just make an ugly hack to be able to get on."
  []

  (defn get-client [] (d/client {:server-type :peer-server
                                 :access-key  "myaccesskey"
                                 :secret      "mysecret"
                                 :endpoint    "localhost:8998"}))

  (defn get-conn "" []
    (d/connect (get-client)  {:db-name "taxonomy_v13"}))

  (defstate ^{:on-reload :noop} conn
    :start (get-conn))

  (let [some-terms        [{:term/base-form "Kontaktmannaskap"}
                           {:term/base-form "Fribrottare"}
                           {:term/base-form "Begravningsentreprenör"}]

        some-concepts     [{:concept/id "MZ6wMoAfyP"
                            :concept/description "grotz"
                            :concept/category :skill
                            :concept/preferred-term [:term/base-form "Kontaktmannaskap"]
                            :concept/alternative-terms #{[:term/base-form "Kontaktmannaskap"]}}
                           {:concept/id "XYZYXYZYXYZ"
                            :concept/description "Fribrottare"
                            :concept/category :occupation
                            :concept/preferred-term [:term/base-form "Fribrottare"]
                            :concept/alternative-terms #{[:term/base-form "Fribrottare"]}}
                           {:concept/id "ZZZZZZZZZZZ"
                            :concept/description "Begravningsentreprenör"
                            :concept/category :occupation
                            :concept/preferred-term [:term/base-form "Begravningsentreprenör"]
                            :concept/alternative-terms #{[:term/base-form "Begravningsentreprenör"]}}]]
    (d/transact (get-conn) {:tx-data (vec (concat some-terms))})
    (d/transact (get-conn) {:tx-data (vec (concat some-concepts))}))

  (get-all-taxonomy-types)
  (get-concepts-for-type :occupation))
;; (stupid-debug)
