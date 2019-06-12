(ns jobtech-taxonomy-api.scratch
  (:require
   [datomic.client.api :as d]
   [mount.core :refer [defstate]]
   )
  )

(def cfg { :datomic-name "taxonomy_v13"
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
  (d/connect (get-client)  {:db-name "taxonomy_v13"} )
  )

(defn get-conn []
  (d/connect (get-client)  {:db-name "jobtech-taxonomy-production"} )
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
  '[:find ?e ?aname ?v ?tx ?added ?inst ?concept-id ?term ?pft
    :where
    [?e ?a ?v ?tx ?added]
    [?a :db/ident ?aname]
    [?e :concept/id ?concept-id]
    [?e :concept/preferred-term ?pft]
    [?pft :term/base-form ?term]
    [?tx :db/txInstant ?inst]
    ]
  )

(def show-deprecated-replaced-by-query
  '[:find (pull ?c
                [
                 :concept/id
                 :concept/description
                 {:concept/preferred-term [:term/base-form]}
                 {:concept/referring-terms [:term/base-form]}
                 {:concept/replaced-by [:concept/id
                                        {:concept/preferred-term [:term/base-form]}

                                        ]}
                 ]) ?inst
    :in $ ?since
    :where
    [?c :concept/deprecated true]
    [?c :concept/replaced-by ?rc ?tx]
    [?tx :db/txInstant ?inst]
    [(< ?since ?inst)]
    ]
  )



(defn  get-db-hist [] (d/history (get-db)))

; (d/q show-concept-history (get-db-hist))

(defn get-concept-history []
  (d/q show-concept-history (get-db-hist))
  )


(defn get-deprecated-replaced-by-since [date-time]
  (d/q show-deprecated-replaced-by-query (get-db) date-time)
  )


(defn get-deprecated-concepts-and-replaced-by []


  )


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

(def term-schema
  [{:db/ident       :term/base-form
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity ; Should this really be unique/identity? Same term can be different concepts. /Sara
    :db/doc         "Term value, the actual text string that is referring to concepts"}
])


(def more-concept-schema
  [{:db/ident       :concept/deprecated
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc         "If a concept is deprecated" ;
    }
   {:db/ident       :concept/replaced-by
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc         "Refers to other concepts that is replacing this one"

    }
   ]
  )


(def some-occupation-names

  [{:concept/id                "1"
    :concept/preferred-term    "term-id-1"
    }
   {:db/id          "term-id-1"
    :term/base-form "Användbarhetsexpert"}
   {:concept/id                "2"
    :concept/preferred-term    "term-id-2"
    }
   {:db/id          "term-id-2"
    :term/base-form "Användbarhetsdesigner"}

   ])

(def substitution-transaction
  [
   {:concept/id "1"
    :concept/deprecated true
    :concept/replaced-by ["3"]
    }
   {:concept/id "2"
    :concept/deprecated true
    :concept/replaced-by ["3"]
    }
   {:db/id                     "3"
    :concept/id                "3"
    :concept/preferred-term    "term-id-3"
    }
   {:db/id          "term-id-3"
    :term/base-form "Interaktionsdesigner"}]
  )


(def add-annonsassistent
  [{:concept/id "4"
    :concept/preferred-term "term-id-4"}

   {:db/id "term-id-4"
    :term/base-form "Annonsassistent"
    }

   ]
  )


(def update-annonsassistent-term
  [{:concept/id "4"
    :concept/preferred-term "term-id-5"
    }
   {:db/id "term-id-5"
    :term/base-form "Annonsassistent/Annonssekreterare"
    }
   ]
  )


#_(def change-preferred-term-transaction
  [{ :concept/id                "3"
    :concept/preferred-term    "term-id-4"
    }
   {:db/id          "term-id-4"
    :term/base-form "Interaktionsdesigner/UX-designer"}]
  )

;; (d/transact conn {:tx-data term-schema})

;; (d/transact conn {:tx-data concept-schema})

;; (d/transact conn {:tx-data more-concept-schema})

;; (d/transact conn {:tx-data some-occupation-names})

;; (d/transact conn {:tx-data substitution-transaction})


;; (d/transact conn {:tx-data add-annonsassistent})

;; (d/transact conn {:tx-data update-annonsassistent-term})

(defn create-new-data-base []

  ;; (d/delete-database (get-client) {:db-name "taxonomy_v13"})
  (d/create-database (get-client) {:db-name "taxonomy_v13"})
  (d/transact (get-conn) {:tx-data term-schema})
  (d/transact (get-conn) {:tx-data concept-schema})
  (d/transact (get-conn) {:tx-data more-concept-schema})
  )




(comment


  Förenkla apiet,
  History apiet har bara deprecate och add. (borde inte deprecate och add också bara visa upp deprecate om add skett och sedan blivit depricatad?)
  Ha ett till API för replaced-by

  Använd deprecated istället för att hela retracta entiteten

  Add och deprecate används av dropdown listor
  Replaced-by används som hjälp när man ska mappa om data i sitt CV, dvs en slagning som kan behöva göras on the fly vid varje upphämtning av CV:t (tvingar en att polla)
  Hämtar alla replaced by som skapats från en viss tidpunkt.


  CREATED  (lägg till ny)
  DEPRECATED ( ogilla)
  UPDATED ( ändra prefered term / lydelse  )

  skapa egen endpoint för detta
  REPLACED_BY ( ge förslag på hänvisningar  )



  )


(comment
  ;;  (def conn (d/connect client {:db-name "demo"}))


  (def old-transaction
    [{:concept/id "1a2s3d4f"
      :concept/description "Kock"
      :concept/category :occupation

      }
     ])

  (def another-old-transaction
    [{:concept/id "22222"
      :concept/description "Bagare"
      :concept/category :occupation

      }
     {:concept/id "3333"
      :concept/description "Betongarbetare"
      :concept/category :occupation
      }
     ])



  (def new-transaction
    [{:concept/id "22222"
      :concept/description "Bagare/Bullmakare"
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

  ;; (d/delete-database (get-client) {:db-name "demo"})



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

(defn group-by-transaction-and-entity [datoms]
  (group-by (juxt #(nth % 3) #(nth % 0) ) datoms)
  )


(defn group-by-attribute [grouped-datoms]
  (map #(group-by second %) grouped-datoms)
  )

#_(defn reduce-datoms-to-event
  "A function that is used with the reduce function to convert a list of datoms from a single transaction into an event."
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


(defn reduce-datoms-to-event
  "A function that is used with the reduce function to convert a list of datoms from a single transaction into an event.  ?e ?aname ?v ?tx ?added ?inst ?concept-id ?term"
  [result datom]
  (let [entity-id (nth datom 0)
        attribute (nth datom 1)
        value (nth datom 2)
        transaction-id (nth datom 3)
        operation (nth datom 4)
        timestamp (nth datom 5)
        concept-id (nth datom 6)
        preferred-term (nth datom 7)

        ]
    (-> result
        (assoc :event/timestamp timestamp)
        (assoc :event/transaction-id transaction-id)
        (update :attribute-values conj [attribute value])
        (assoc attribute value)
        (update-in [:operations attribute] conj operation)
        (assoc :entity-id entity-id)
        (assoc :concept/id concept-id)
        (assoc :concept/preferred-term preferred-term)

        )
    )
  )


;; blir knas med anvandbarhetsexpert

#_(defn determine-event-type [reduced-datoms]
  (let [operations (map #(second %) (:operations reduced-datoms))
        all-true (every? true? operations)
        all-false (every? false? operations)
        event-type (cond
                     all-true  {:event-type "CREATED"}
                     all-false {:event-type "DEPRECATED"}
                     :else     {:event-type "UPDATED"}
                     )
        ]
    (-> reduced-datoms
        (merge event-type)
        (dissoc :operations)
        )
    )
  )



(defn filter-duplicate-preferred-term-datoms [[_ _ preferred-term-id _ operation _ _ preferred-term preferred-term-id-again]]
  (= preferred-term-id preferred-term-id-again)
  )

(defn keep-after-update [[_ _ _ _ operation]]
  operation
  )



(defn is-event-update-preferred-term? [datoms-grouped-by-attribute]
  "checks if op is not all true or false"
  (if-let [datoms (:concept/preferred-term datoms-grouped-by-attribute)]
    (not (apply = (map #(nth % 4) datoms)))
    false
    )
  )

(defn is-event-create-concept? [datoms-grouped-by-attribute]
  (if-let [datoms (:concept/id datoms-grouped-by-attribute)]
    (every? true? (map #(nth % 4) datoms))
    false
    )
  )

(defn is-event-deprecated-concept? [datoms-grouped-by-attribute]
  (if-let [datoms (:concept/deprecated datoms-grouped-by-attribute)]
    (every? true? (map #(nth % 4) datoms))
    false
    )
  )



#_(defn determine-event-type [reduced-datom]
  (let [is-created (every? true? (get-in reduced-datom [:operations :concept/id]))
        is-deprecated (every? true? (get-in reduced-datom [:operations :concept/deprecated]))

        event-type (cond
                     is-created {:event/type "CREATED"}
                     is-deprecated {:event/type "DEPRECATED"}

                     )
        result-with-event (merge event-type reduced-datom)
        ]
    (-> result-with-event
        (dissoc :operations)
        (dissoc :entity-id)
        (dissoc :concept/replaced-by) ;; se hur vi gör framöver

        )
    )
  )

(defn create-event-create-concept-from-datom [datoms-grouped-by-attribute]

  "TODO fix potential bugfest, first is a bit sketchy"
  (let [[_ _ _ transaction-id _ timestamp concept-id preferred-term]  (first (filter filter-duplicate-preferred-term-datoms (:concept/preferred-term datoms-grouped-by-attribute)))]
    {:event-type "CREATED"
     :transaction-id transaction-id
     :timestamp timestamp
     :concept-id concept-id
     :preferred-term preferred-term
     }
    )
  )

(defn create-event-deprecated-concept-from-datom [datoms-grouped-by-attribute]
  (let [[_ _ _ transaction-id _ timestamp concept-id preferred-term]  (first (:concept/deprecated datoms-grouped-by-attribute))]
    {:event-type "DEPRECATED"
     :transaction-id transaction-id
     :timestamp timestamp
     :concept-id concept-id
     :preferred-term preferred-term
     :deprecated true
     }
    )
  )

(defn create-event-updated-preferred-term [datoms-grouped-by-attribute]
  (let [datoms (filter filter-duplicate-preferred-term-datoms (:concept/preferred-term datoms-grouped-by-attribute))
        datom-before (filter #(not (keep-after-update %)) datoms)
        datom-after  (filter keep-after-update datoms)
        [[_ _ _ _ _ timestamp concept-id old-preferred-term]] datom-before
        [[_ _ _ transaction-id _ _ _ new-preferred-term]] datom-after
        ]
    {:event-typ "UPDATED"
     :transaction-id transaction-id
     :timestamp timestamp
     :concept-id concept-id
     :old-preferred-term old-preferred-term
     :new-preferred-term new-preferred-term
     }
    )
  )

(defn determine-event-type [datoms-by-attibute]
  (let [is-event-create-concept (is-event-create-concept? datoms-by-attibute)
        is-event-deprecated-concept (is-event-deprecated-concept? datoms-by-attibute)
        is-event-update-preferred-term (is-event-update-preferred-term? datoms-by-attibute)
        ]

    (cond
      is-event-create-concept (create-event-create-concept-from-datom datoms-by-attibute)
      is-event-deprecated-concept (create-event-deprecated-concept-from-datom datoms-by-attibute)
      is-event-update-preferred-term (create-event-updated-preferred-term datoms-by-attibute)
      )
    )
  )





(defn convert-history-to-events [datoms]
  (let [grouped-datoms (map second (group-by-transaction-and-entity datoms))
        datoms-by-attibute (group-by-attribute grouped-datoms)
        events (map determine-event-type datoms-by-attibute)
        ]
    events
    )
  )


(defn convert-all []
  (sort-by :timestamp
           (convert-history-to-events
            (d/q show-concept-history (get-db-hist)))))


(comment

  (def datoms (d/q show-concept-history (get-db-hist)))
  (def grouped-datoms  (map second (group-by-transaction-and-entity datoms)))
 ;; (def reduced-datoms   (map #(reduce reduce-datoms-to-event {:concept/preferred-term []} %)  grouped-datoms)
  (def datoms-by-attibute (group-by-attribute grouped-datoms))

    )



;; Create om det bara finns true på operations - stämmer ej längre
;; Delete om det finns true på concept id - stämmer ej längre
;; Update om det finns true och false på samma attribut i samma transaction - stämmer ej längre


;; Skapa event ström som som visar transactionsid och entitets id
;; Sen hydradar med as of frågor mot databasen för att få ut mer info om conceptet

;; Create om det finns true på concept id
;; Depreaate om det finns true på concept/deprecate

;; Udate om det finns true och false på term/baseford

;; Gör koll så att man inte kan lägga till Concept utan prefered term

(def datoms-update-preferred-term

  #:concept{
            :preferred-term
             [[7327145487499336
             :concept/preferred-term
             70746976177619018
             13194139533320
             true
             #inst "2019-02-16T17:10:51.894-00:00"
             "4"
             "Annonsassistent"
             32136525856637001]
            [7327145487499336
             :concept/preferred-term
             32136525856637001
             13194139533320
             false
             #inst "2019-02-16T17:10:51.894-00:00"
             "4"
             "Annonsassistent"
             32136525856637001]
            [7327145487499336
             :concept/preferred-term
             70746976177619018
             13194139533320
             true
             #inst "2019-02-16T17:10:51.894-00:00"
             "4"
             "Annonsassistent/Annonssekreterare"
             70746976177619018]
            [7327145487499336
             :concept/preferred-term
             32136525856637001
             13194139533320
             false
             #inst "2019-02-16T17:10:51.894-00:00"
             "4"
             "Annonsassistent/Annonssekreterare"
             70746976177619018]]}

  )

(comment

  )


;; innehaller preffered-term op som ar olie  =  update
;; innehaller id op true = create
;; innehaller deprecated op true = deprecate


(def countries-and-continents
  [
   {:continent-id 2 :continent-name "EU"  :country-id 4 :country-name "Sweden"  }
   {:continent-id 2 :continent-name "EU"  :country-id 5 :country-name "Finlad"  }
   ]
  )


(defn create-continent-temp-id [id]
  (str "continent-" id)
  )

(defn create-continent [id name]
  {:id id :name name}
  )

(defn lookup-continent [result continent-id continent-name]

  (let [temp-id (create-continent-temp-id continent-id)
        continent (or (get temp-id result)  (create-continent continent-id continent-name)  )
        ]
    (assoc result temp-id continent)
    )

  )

(defn reduce-converter [result item]

  (let [country-id (:country-id item )
        country-name (:country-name item )
        continent-id (:continent-id item )
        continent-name (:continent-name item )
        new-result  (lookup-continent result continent-id continent-name)
        ]
    (assoc new-result country-id {:id country-id :name country-name :continent-id (create-continent-temp-id continent-id)  })
    )
  )
