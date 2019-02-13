(ns jobtech-taxonomy-api.scratch
  (:require
   [datomic.client.api :as d]
   [mount.core :refer [defstate]]

   )
  )


(def cfg { :datomic-name "demo"
       :datomic-cfg {
                    :server-type :ion
                    :region "eu-west-1" ;; e.g. us-east-1
                    :system "prod-jobtech-taxonomy-db"
                    ;;:creds-profile "<your_aws_profile_if_not_using_the_default>"
                    :endpoint "http://entry.prod-jobtech-taxonomy-db.eu-west-1.datomic.net:8182/"
                    :proxy-port 8182}

      })


(defn get-client [] (d/client (:datomic-cfg cfg)))


(def  conn
   (d/connect (get-client)  {:db-name "demo"} )
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

(def show-concept-history
  '[:find ?e ?aname ?v ?tx ?added ?inst
    :where
    [?e ?a ?v ?tx ?added]
    [?a :db/ident ?aname]
    [?e :concept/id]
    [?tx :db/txInstant ?inst]
    ]
  )


(defn  get-db-hist [] (d/history (get-db)))

; (d/q show-concept-history (get-db-hist))


;; DEMO
(def database-query

  '[:find ?attr ?type ?card
   :where
   [_ :db.install/attribute ?a]
   [?a :db/valueType ?t]
   [?a :db/cardinality ?c]
   [?a :db/ident ?attr]
   [?t :db/ident ?type]
   [?c :db/ident ?card]])

;; (d/q database-query (get-db))


  (def concept-schema
    [{:db/ident       :concept/id
      :db/valueType   :db.type/string
      :db/cardinality :db.cardinality/one
      :db/unique      :db.unique/identity
      :db/doc         "Unique identifier for concepts"}

     {:db/ident       :concept/description
      :db/valueType   :db.type/string
      :db/cardinality :db.cardinality/one
      :db/doc         "Text describing the concept, is used for disambiguation."}

     {:db/ident       :concept/preferred-term
      :db/valueType   :db.type/ref
      :db/cardinality :db.cardinality/one
      :db/doc         "What we prefer to call the concept"}

     {:db/ident       :concept/alternative-terms
      :db/cardinality :db.cardinality/many
      :db/valueType   :db.type/ref
      :db/doc         "All terms referring to this concept"}

     {:db/ident       :concept/category
      :db/valueType   :db.type/keyword
      :db/cardinality :db.cardinality/one
      :db/doc         "JobTech categories" ;
      }

     ]


    )

(def more-concept-schema
  [{:db/ident       :concept/deprecated
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc         "If a concept is deprecated" ;
    }]
  )


;; (d/transact conn {:tx-data concept-schema})
;; (d/transact conn {:tx-data more-concept-schema})




(comment
  ;;  (def conn (d/connect client {:db-name "demo"}))


  (def old-transaction
    [{:concept/id "1a2s3d4f"
      :concept/description "Kock"
      :concept/category :occupation

      }
     ])


  (def new-transaction
    [{:concept/id "1a2s3d4f"
      }
     ])


  (d/transact conn {:tx-data old-transaction})
  (d/transact conn {:tx-data new-transaction})
  ;; Retracta en entitet
  ;;  (d/transact conn {:tx-data [[:db/retractEntity [:concept/id "1a2s3d4f"]]] })
  (d/q find-by-id (get-db) "1a2s3d4f")



  (def find-by-id '[:find (pull ?c
                                [ :concept/id
                                                           :concept/description
                                                           :concept/category
                                                           :db/txInstant
                                                           ])
                                              :in $ _
                                              :where [?c :concept/id _]


                                              ])



  (show-term-history-since #inst "2019-02-10")

  ;; (d/create-database (get-client) {:db-name "demo"})

;;(d/delete-database (get-client) {:db-name "demo"})



  )


(def add-and-delete-concept-history

  [[59052570504593477
    :concept/category
    :occupation
    13194139533318
    false
    #inst "2019-02-12T14:30:04.086-00:00"]
   [59052570504593477
    :concept/description
    "Kock"
    13194139533317
    true
    #inst "2019-02-12T14:12:10.341-00:00"]
   [59052570504593477
    :concept/id
    "1a2s3d4f"
    13194139533317
    true
    #inst "2019-02-12T14:12:10.341-00:00"]
   [59052570504593477
    :concept/category
    :occupation
    13194139533317
    true
    #inst "2019-02-12T14:12:10.341-00:00"]
   [59052570504593477
    :concept/description
    "Kock"
    13194139533318
    false
    #inst "2019-02-12T14:30:04.086-00:00"]
   [59052570504593477
    :concept/id
    "1a2s3d4f"
    13194139533318
    false
    #inst "2019-02-12T14:30:04.086-00:00"]]

  )


;; (group-by #(nth % 3) add-and-delete-concept-history)

(defn group-by-transaction [datoms]
  "TODO group on entity id aswell"
  (group-by #(nth % 3) datoms)
  )

(defn reduce-datoms-to-event
  "A function that is used with the reduce function to convert a list of datoms from a single transaction into a event."
  [result datom]
  (let [attribute (nth datom 1)
        value (nth datom 2)
        operation (nth datom 4)
        timestamp (nth datom 5)
        ]
    (-> result
        (assoc attribute value)
        (assoc :timestamp timestamp)
        (update :operations conj [attribute operation])
        (assoc :timestamp timestamp)
        )
    )
  )


(defn determine-event-type [reduced-datoms]
  (let [operations (map #(second %) (:operations reduced-datoms))
        all-true (every? true? operations)
        all-false (every? false? operations)
        ]
    (cond
      all-true  {:event-type "CREATE"}
      all-false {:event-type "DELETE"}
      :else     {:event-type "UPDATE"}
      )
    )
  )



;; Create om det bara finns true på operations
;; Delete om det finns true på concept id
;; Update om det finns true och false på samma attribut i samma transaction
