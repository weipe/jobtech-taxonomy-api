(ns jobtech-taxonomy-api.db.core
  (:require
   [datomic.client.api :as d]
   [schema.core :as s]
   [clojure.test :refer [is]]
   [clj-time.coerce :as c]
   [clj-time [format :as f]]
   [clojure.set :as set]
   [mount.core :refer [defstate]]
   [jobtech-taxonomy-api.config :refer [env]]
   [jobtech-taxonomy-database.nano-id :as nano]
   [jobtech-taxonomy-api.db.events :refer :all]))

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
  '[:find (pull ?c [:concept/id
                    :concept/description
                    :concept/category
                    :concept/deprecated
                    {:concept/preferred-term [:term/base-form]}
                    {:concept/referring-terms [:term/base-form]}
                    ] )
    :in $ ?term
    :where [?t :term/base-form ?term]
    [?c :concept/preferred-term ?t]])

(def find-concept-by-preferred-term-schema
  "The response schema for the query below."
  {:id s/Str
   :definition s/Str
   :instanceType s/Str
   (s/optional-key :deprecated) s/Bool})

(defn rename-concept-keys-for-api [concept]
  (set/rename-keys concept {:concept/preferred-term :preferredLabel, :concept/id :id, :concept/description :definition, :concept/category :type :concept/deprecated :deprecated}))

(defn lift-term [concept]
  (assoc (dissoc concept :preferred-term)
         :concept/preferred-term (get-in concept [:concept/preferred-term :term/base-form] )))

(defn parse-find-concept-datomic-result [result]
  (->> result
       (map first)
       (map #(lift-term %))
       (map #(update % :concept/category name))
       (map rename-concept-keys-for-api)
       )
  )

(defn find-concept-by-preferred-term [term]
  "Lookup concepts by term. Special term '___THROW_EXCEPTION' throws an exception, handy for testning error handling."
  {:pre  [(is (and (not (nil? term)) (> (count term) 0))  "supply a non-empty string argument")]}
  (if (= term "___THROW_EXCEPTION")
    (throw (NullPointerException. "Throwing test exception.")))
  (let [result (d/q find-concept-by-preferred-term-query (get-db) term)]
    (parse-find-concept-datomic-result result)
    )
  )

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

(def find-concepts-schema
  "The response schema for /concepts. Beta for v0.9."
    [{ :id s/Str
       :type s/Str
       :definition s/Str
       :preferredLabel s/Str }])

#_(defn find-concepts [id preferred-label type deprecated offset limit]
  "Beta for v0.9."
  '({ :id "Vpaw_yX7_BNY",
      :preferredLabel "Sportdykning",
      :type :skill }))

(defn retract-concept [id]
  (let [retract (d/transact (get-conn) {:tx-data
                                        [{:concept/id id
                                          :concept/deprecated true}]})]

    {:msg (if retract "ok" "bad")}))


(def get-relation-graph-query
  '[:find ?c1id ?c2id
    :in $ ?relation-type
    :where
    [?re :relation/type      ?relation-type]
    [?re :relation/concept-1 ?c1]
    [?re :relation/concept-2 ?c2]
    [?c1 :concept/id ?c1id]
    [?c2 :concept/id ?c2id]])

(def get-relation-graph-from-concept-query-rules
  '[[(related-concepts ?c1 ?c2 ?t)
     [?re :relation/type      ?t]
     [?re :relation/concept-1 ?c1]
     [?re :relation/concept-2 ?c2]]])

(def get-relation-graph-from-concept-query
  '[:find (pull ?child [:db/id
                        :concept/id
                        {:concept/preferred-term [:term/base-form]}])
    :in $ % ?relation-type ?id :where
    [?parent :concept/id ?id]
    (related-concepts ?parent ?child ?relation-type)])

(def map-id-to-term-query
  '[:find ?term
    :in $ ?id
    :where
    [?ce :concept/id ?id]
    [?ce :concept/preferred-term ?te]
    [?te :term/base-form ?term]])

; (find-concept-by-id "aqxj_t1i_SxL") ; Sydsudan
; (find-concept-by-id "Gk4Z_5LP_v5G") ; Nordafrika

(defn map-id-to-term [id]
  "Return the preferred label for a given ID. The query returns a list of lists, therefore we do an (ffirst)."
  (ffirst (d/q map-id-to-term-query (get-db) id)))

(defn get-relation-graph [relation-type]
  (letfn [(lazy-contains? [col key]
            (some #{key} col))
          (get-children [n all-pairs]
            (flatten (map vals (filter #(contains? % n) all-pairs))))
          (get-hier-rec [parent all-pairs]
            (let [children (get-children parent all-pairs)
                  term (map-id-to-term parent)]
              (if (empty? children)
                { "name" term, "id" parent }
                { "name" term, "id" parent, "children" (map #(get-hier-rec % all-pairs) children) })))]
    (let [list (d/q get-relation-graph-query (get-db) relation-type)
          map-key-val (map #(apply hash-map % ) list)
          map-val-key (map #(apply hash-map (reverse %) ) list)
          tops (->> (map #(first (keys %)) map-key-val)
                    (filter #(not (lazy-contains? map-val-key %)) ,,,)
                    (distinct ,,,))]
      { "name" "Jobtech", "id" "666", "children" (map #(get-hier-rec % map-key-val) tops) })))

(defn get-relation-graph-from-concept [relation-type id]
  (letfn [(format-answer [[list]]
            { "name" (get-in list [:concept/preferred-term :term/base-form]) "id" (get-in list [:concept/id])})]
    (let [lookup (d/q get-relation-graph-from-concept-query (get-db) get-relation-graph-from-concept-query-rules relation-type id)]
      { "name" (map-id-to-term id), "id" id, "children" (map #(format-answer %) lookup) })))

(def get-relation-types-query
  '[:find ?v :where [_ :relation/type ?v]])


(defn get-relation-types []
  (->> (d/q get-relation-types-query (get-db))
       (sort-by first)
       (apply concat)))

;; TODO appeda pa replaced by listan


(defn replace-deprecated-concept [old-concept-id new-concept-id]
  (let [data {:concept/id old-concept-id
              :concept/replaced-by [{:concept/id new-concept-id}]}
        result (d/transact (get-conn) {:tx-data [data]})
        timestamp (nth (first (:tx-data result)) 2)]

    {:msg (if result {:timestamp timestamp :status "OK"} {:status "ERROR"})}))

(defn assert-concept-part [type desc pref-term]
  (let* [temp-id   (format "%s-%s-%s" type desc pref-term)
         tx        [{:db/id temp-id
                     :term/base-form pref-term}
                    {:concept/id (nano/generate-new-id-with-underscore)
                     :concept/description desc
                     :concept/category (keyword (str type))
                     :concept/preferred-term temp-id
                     :concept/alternative-terms #{temp-id}}]
         result     (d/transact (get-conn) {:tx-data (vec (concat tx))})]
        result))

(defn assert-concept "" [type desc pref-term]
  (let [result (assert-concept-part type desc pref-term)
        timestamp (nth (first (:tx-data result)) 2)]

    {:msg (if result {:timestamp timestamp :status "OK"} {:status "ERROR"})}))

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

(def show-concept-events-schema
  "The response schema for the query below."
  [{:event-type s/Str
    :transaction-id s/Int
    :timestamp java.util.Date
    :concept-id s/Str
    :category s/Keyword
    (s/optional-key :preferred-term) s/Str
    (s/optional-key :old-preferred-term) s/Str
    (s/optional-key :new-preferred-term) s/Str
    (s/optional-key :deprecated) s/Bool}])

(def show-changes-schema
  "The response schema for /changes. Beta for v0.9."
  [{:eventType s/Str
    :transactionId s/Int
    :timestamp java.util.Date
    :concept { :id s/Str
              :type s/Str
              (s/optional-key :preferredLabel) s/Str }}])

(defn show-concept-events []
  (get-all-events (get-db)))

(defn show-concept-events-since [date-time]
  (get-all-events-since (get-db) date-time))

(defn show-changes-since [date-time offset limit]
  "Show changes since a specific time. Beta for v0.9."
  (get-all-events-since-v0-9 (get-db) date-time offset limit))

(defn show-deprecated-concepts-and-replaced-by [date-time]
  (get-deprecated-concepts-replaced-by-since (get-db) date-time))

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

(defn ignore-case [string]
  (str "(?i:.*" string  ".*)"))

(def find-concepts-by-term-start-query
  '[:find (pull ?c [:concept/id
                    :concept/description
                    :concept/category
                    :concept/deprecated
                    {:concept/preferred-term [:term/base-form]}
                    {:concept/referring-terms [:term/base-form]}
                    ] )
    :in $ ?letter
    :where [?c :concept/preferred-term ?t]
    [?t :term/base-form ?term]
    ;;[(.startsWith ^String ?term ?letter)]
    [(.matches ^String ?term ?letter)]])



(def find-concepts-by-term-start-type-query
  '[:find (pull ?c [:concept/id
                    :concept/description
                    :concept/category
                    :concept/deprecated
                    {:concept/preferred-term [:term/base-form]}
                    {:concept/referring-terms [:term/base-form]}
                    ] )
    :in $ ?letter ?type
    :where
    [?c :concept/category ?type]
    [?c :concept/preferred-term ?t]
    [?t :term/base-form ?term]
    [(.matches ^String ?term ?letter)]])


(def get-concepts-by-term-start-schema
  "The response schema for the query below."
  [{:id s/Str
    :definition s/Str
    :type s/Str
    (s/optional-key :preferredLabel) s/Str
    (s/optional-key :deprecated) s/Bool}])






(defn get-concepts-by-term-start [letter]
  (parse-find-concept-datomic-result (d/q find-concepts-by-term-start-query (get-db) (ignore-case letter)))
  )

(defn get-concepts-by-term-start-type [letter type]
  (parse-find-concept-datomic-result (d/q find-concepts-by-term-start-type-query (get-db) (ignore-case letter) type))
  )


#_(def get-concepts-by-term-start-schema
  "The response schema for the query below. Beta for v0.9."
  [{:id s/Str
    :definition s/Str
    :preferredLabel s/Str
    :type s/Str}])


#_(defn get-concepts-by-search [q type offset limit]
  "Beta for v0.9."
  '({ :id "Vpaw_yX7_BNY"
     :preferredLabel "Sportdykning"
     :type :skill }))



(comment
  (defn get-concepts [id label type]

    cond  iff id


    )

  )
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;




(def fetch-concepts-by-preferred-term-query
  '[:find (pull ?c [:concept/id
                    :concept/description
                    :concept/category
                    :concept/deprecated
                    {:concept/preferred-term [:term/base-form]}
                    {:concept/referring-terms [:term/base-form]}
                    ] )
    :in $ ?term
    :where [?t :term/base-form ?term]
    [?c :concept/preferred-term ?t]])

(def fetch-concepts-by-id-query
  '[:find (pull ?c [:concept/id
                    :concept/description
                    :concept/category
                    :concept/deprecated
                    {:concept/preferred-term [:term/base-form]}
                    {:concept/referring-terms [:term/base-form]}
                    ] )
    :in $ ?id
    :where
    [?c :concept/id ?id]])

(def fetch-concepts-by-preferred-label-type-deprecated-query
  '[:find (pull ?c [:concept/id
                    :concept/description
                    :concept/category
                    :concept/deprecated
                    {:concept/preferred-term [:term/base-form]}
                    {:concept/referring-terms [:term/base-form]}
                    ] )
    :in $ ?label ?type ?deprecated
    :where
    [?c :concept/preferred-term ?pt]
    [?pt :term/base-form ?label]
    [?c :concept/category ?type]
    [?c :concept/deprecated ?deprecated]
    ])


(def fetch-concepts-by-preferred-label-type-query
  '[:find (pull ?c [:concept/id
                    :concept/description
                    :concept/category
                    :concept/deprecated
                    {:concept/preferred-term [:term/base-form]}
                    {:concept/referring-terms [:term/base-form]}
                    ] )
    :in $ ?label ?type
    :where
    [?c :concept/preferred-term ?pt]
    [?pt :term/base-form ?label]
    [?c :concept/category ?type]
    ])

(def fetch-concepts-by-preferred-label-deprecated-query
  '[:find (pull ?c [:concept/id
                    :concept/description
                    :concept/category
                    :concept/deprecated
                    {:concept/preferred-term [:term/base-form]}
                    {:concept/referring-terms [:term/base-form]}
                    ] )
    :in $ ?label ?deprecated
    :where
    [?c :concept/preferred-term ?pt]
    [?pt :term/base-form ?label]
    [?c :concept/deprecated ?deprecated]
    ])

(def fetch-concepts-by-type-deprecated-query
  '[:find (pull ?c [:concept/id
                    :concept/description
                    :concept/category
                    :concept/deprecated
                    {:concept/preferred-term [:term/base-form]}
                    {:concept/referring-terms [:term/base-form]}
                    ] )
    :in $ ?type ?deprecated
    :where
    [?c :concept/category ?type]
    [?c :concept/deprecated ?deprecated]
    ])

(def fetch-concepts-by-type-query
  '[:find (pull ?c [:concept/id
                    :concept/description
                    :concept/category
                    :concept/deprecated
                    {:concept/preferred-term [:term/base-form]}
                    {:concept/referring-terms [:term/base-form]}
                    ] )
    :in $ ?type
    :where
    [?c :concept/category ?type]
    ])

(def fetch-concepts-by-preferred-label-query
  '[:find (pull ?c [:concept/id
                    :concept/description
                    :concept/category
                    :concept/deprecated
                    {:concept/preferred-term [:term/base-form]}
                    {:concept/referring-terms [:term/base-form]}
                    ] )
    :in $ ?label
    :where
    [?c :concept/preferred-term ?pt]
    [?pt :term/base-form ?label]
    ])

(def fetch-concepts-by-deprecated-query
  '[:find (pull ?c [:concept/id
                    :concept/description
                    :concept/category
                    :concept/deprecated
                    {:concept/preferred-term [:term/base-form]}
                    {:concept/referring-terms [:term/base-form]}
                    ] )
    :in $ ?deprecated
    :where
    [?c :concept/deprecated ?deprecated]
    ])


(defn fetch-concepts-choose-query [id preferred-label type deprecated]
  (cond
    id (d/q  fetch-concepts-by-id-query (get-db) id )
    (and preferred-label  type deprecated) (d/q fetch-concepts-by-preferred-label-type-deprecated-query (get-db) preferred-label type deprecated)
    (and preferred-label type) (d/q fetch-concepts-by-preferred-label-type-query (get-db) preferred-label type )
    (and preferred-label deprecated) (d/q fetch-concepts-by-preferred-label-deprecated-query (get-db) preferred-label deprecated )
    (and type deprecated) (d/q fetch-concepts-by-type-deprecated-query (get-db) type deprecated )
    preferred-label (d/q fetch-concepts-by-preferred-label-query (get-db) preferred-label)
    type (d/q fetch-concepts-by-type-query (get-db) type)
    deprecated (d/q fetch-concepts-by-deprecated-query (get-db) deprecated)
    :else "error"
    )
  )

(defmacro pagination
  "Pagination mimicking the MySql LIMIT"
  ([coll start-from quantity]
   `(take ~quantity (drop ~start-from ~coll)))
  ([coll quantity]
   `(pagination ~coll 0 ~quantity)))


(defn empty-string-to-nil [string]
  (if (seq string)
    string
    nil
    )
  )



(defn paginate-datomic-result [result offset limit]
  (cond
    (and (= 0 offset) (= 0 limit)) (pagination result 0 100)
    (and offset limit) (pagination result offset limit)
    offset (drop offset result)
    limit (take limit result)
    :else (pagination result 0 100)
    )
  )

(defn find-concepts [id preferred-label type deprecated offset limit]
  "Beta for v0.9."
  (let [datomic-result (fetch-concepts-choose-query (empty-string-to-nil id)
                                            (empty-string-to-nil preferred-label)
                                            (keyword (empty-string-to-nil type))
                                            deprecated)
        result (parse-find-concept-datomic-result datomic-result)
        ]
    (paginate-datomic-result result offset limit)
    )
  )



;;;;;;;;;;; search


(defn get-concepts-by-search [q type offset limit]
  "Beta for v0.9."
  (let [result (cond
                         (and (empty-string-to-nil q) (empty-string-to-nil type)) (get-concepts-by-term-start-type q (keyword type))
                         (empty-string-to-nil q) (get-concepts-by-term-start q)
                         :else "error"
                         )]
    (paginate-datomic-result result offset limit)
    )
  )





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

;;  time datbase query  (time (get-all-events-since (get-db) #inst "2019-04-10"))
;; if you want to get less output do   (let [_ (time (get-all-events-since (get-db) #inst "2019-04-10" ))])
