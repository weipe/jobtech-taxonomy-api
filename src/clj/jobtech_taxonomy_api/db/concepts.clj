(ns jobtech-taxonomy-api.db.concepts
  (:refer-clojure :exclude [type])
  (:require
   [schema.core :as s]
   [datomic.client.api :as d]
   [jobtech-taxonomy-api.db.database-connection :refer :all]
   [jobtech-taxonomy-api.db.api-util :refer :all]
   [clojure.set :as set]
   ))

(comment
  "To understand the idea behind the following code read this blog post
   https://grishaev.me/en/datomic-query/ "
  )

(def initial-concept-query
  '{:find [(pull ?c [:concept/id
                     :concept/type
                     :concept/definition
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

(defn find-concepts [id preferred-label type deprecated offset limit]
  "Beta for v0.9."
  (let [result (d/q (fetch-concepts id preferred-label type deprecated offset limit))
        parsed-result (parse-find-concept-datomic-result result)]
    parsed-result
    )
  )

(def find-concepts-schema
  "The response schema for /concepts. Beta for v0.9."
  [{ :id s/Str
    :type s/Str
    :definition s/Str
    :preferredLabel s/Str
    (s/optional-key :deprecated) s/Bool
    }
   ])
