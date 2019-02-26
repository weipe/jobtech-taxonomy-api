(ns jobtech-taxonomy-api.db.core
  (:require
   [datomic.client.api :as d]
   [mount.core :refer [defstate]]
   [jobtech-taxonomy-api.config :refer [env]]
   [jobtech-taxonomy-api.nano-id :refer :all]
   [jobtech-taxonomy-api.db.events :refer [get-all-events get-all-events-since get-deprecated-concepts-replaced-by-since]]
   ))

#_(defstate conn
    :start (-> env :database-url d/connect)
    :stop (-> conn .release))

(defn get-client [] (d/client (:datomic-cfg env)))

(defstate ^{:on-reload :noop} conn
  :start (d/connect (get-client)  {:db-name (:datomic-name env)}))


(defn get-db [] (d/db conn))

(defn get-conn "" []
  (d/connect (get-client)  {:db-name (:datomic-name env)}))


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

(defn retract-concept [id]
  (let [retract (d/transact (get-conn) {:tx-data

                                        [
                                         {:concept/id id
                                          :concept/deprecated true
                                          }


                                         ] })]
    { :msg (if retract "ok" "bad") }))



;; TODO appeda pa replaced by listan
(defn replace-deprecated-concept [old-concept-id new-concept-id]
  (let [data {:concept/id old-concept-id
              :concept/replaced-by [{:concept/id new-concept-id}]
              }
        result (d/transact (get-conn) {:tx-data [data]})
        timestamp (nth (first (:tx-data result)) 2 )
        ]

    { :msg (if result {:timestamp timestamp :status "OK"} {:status "ERROR"} ) }
    )

  )


(defn assert-concept-part [type desc pref-term]
  (let* [temp-id   (format "%s-%s-%s" type desc pref-term)
         tx        [{:db/id temp-id
                     :term/base-form pref-term}
                    {:concept/id (generate-new-id)
                     :concept/description desc
                     :concept/category (keyword (str type))
                     :concept/preferred-term temp-id
                     :concept/alternative-terms #{temp-id}}]
         result     (d/transact (get-conn) {:tx-data (vec (concat tx))})]
    result)
  )


(defn assert-concept "" [type desc pref-term]
  (let [result (assert-concept-part type desc pref-term)
        timestamp (nth (first (:tx-data result)) 2 )
        ]

    { :msg (if result {:timestamp timestamp :status "OK"} {:status "ERROR"} ) }))




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
(d/q get-concepts-for-type-query (get-db) (keyword type)))

(def get-all-taxonomy-types-query
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

(defn show-concept-events []
  (get-all-events (get-db))
  )

(defn show-concept-events-since [date-time]
  (get-all-events-since (get-db) date-time)
  )

(defn show-deprecated-concepts-and-replaced-by [date-time]
  (get-deprecated-concepts-replaced-by-since (get-db) date-time)
  )


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

(def conn (get-conn))

(defn get-db [] (d/db conn))

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
(get-concepts-for-type :occupation)
(assert-concept "skill" "weqweqw" "fsdfsdfsd")
;; (retract-concept "ZZZZZZZZZZZ")
)
;; (stupid-debug)
