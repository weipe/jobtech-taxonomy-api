(ns jobtech-taxonomy-api.db.concepts
  (:require
   [datomic.client.api :as d]
   [jobtech-taxonomy-database.nano-id :as nano]
   [jobtech-taxonomy-api.db.database-connection :refer :all]
   [jobtech-taxonomy-api.db.api-util :refer :all]
   [clojure.set :as set]
   ))

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
  (let [existing (find-concepts nil pref-term type nil nil nil)]
    (if (> (count existing) 0)
      [false nil]
      (let [result (assert-concept-part type desc pref-term)
            timestamp (if result (nth (first (:tx-data result)) 2) nil)]
        [result timestamp]))))
