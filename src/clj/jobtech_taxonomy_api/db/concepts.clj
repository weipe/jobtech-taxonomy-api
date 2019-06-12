(ns jobtech-taxonomy-api.db.concepts
  (:refer-clojure :exclude [type])
  (:require
   [datomic.client.api :as d]
   [jobtech-taxonomy-api.db.database-connection :refer :all]
   [jobtech-taxonomy-api.db.api-util :refer :all]
   [clojure.set :as set]
   ))

;;

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
    (and preferred-label type deprecated) (d/q fetch-concepts-by-preferred-label-type-deprecated-query (get-db) preferred-label type deprecated)
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



(comment
  "To understand the idea behind the following code read this blog post
   https://grishaev.me/en/datomic-query/ "
  )

(def initial-concept-query
  '{:find [(pull ?c [:concept/id
                     :concept/type
                     :concept/description
                     :concept/preferred-label
                     :concept/deprecated
                     ])]
    :in [$]
    :args []
    :where []
    :offset 0
    :limit -1
    })


(defn remap-query
  [{args :args offset :offset limit :limit :as m}]
  {:query (-> m
              (dissoc :args)
              (dissoc :offset)
              (dissoc :limit)
              )
   :args args
   :offset offset
   :limit limit
   }
  )

(defn fetch-concepts [id preferred-label type deprecated offset limit]

  (cond-> initial-concept-query

    true
    (update :args conj (get-db))

    id
    (-> (update :in conj '?id)
        (update :args conj id)
        (update :where conj '[?c :concept/id ?id])
        )

    preferred-label
    (-> (update :in conj '?preferred-label)
        (update :args conj preferred-label)
        (update :where conj '[?c :concept/preferred-label ?preferred-label])
        )

    type
    (-> (update :in conj '?type)
        (update :args conj type)
        (update :where conj '[?c :concept/type ?type])
        )

    deprecated
    (-> (update :in conj '?deprecated)
        (update :args conj deprecated)
        (update :where conj '[?c :concept/deprecated ?deprecated])
        )

    offset
    (assoc :offset offset)

    limit
    (assoc :limit limit)

    true
    remap-query
    )
  )
