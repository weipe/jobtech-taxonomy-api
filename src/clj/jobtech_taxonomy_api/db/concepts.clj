(ns jobtech-taxonomy-api.db.concepts
  (:refer-clojure :exclude [type])
  (:require
   [schema.core :as s]
   [datahike.api :as d]
   [jobtech-taxonomy-database.nano-id :as nano]
   [jobtech-taxonomy-api.db.database-connection :refer :all]
   [jobtech-taxonomy-api.db.api-util :refer :all]
   [jobtech-taxonomy-api.db.api-util :as api-util]
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
                     {:concept/replaced-by [:concept/id
                                            :concept/definition
                                            :concept/type
                                            :concept/preferred-label
                                            :concept/deprecated
                                            ]}
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

(defn fetch-concepts [id preferred-label type deprecated offset limit db]

  (cond-> initial-concept-query

    true
    (update :args conj db)

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

(defn find-concepts-by-db
  ([id preferred-label type deprecated offset limit db]
   (let [ result (d/q (fetch-concepts id preferred-label type deprecated offset limit db))
         parsed-result (parse-find-concept-datomic-result result)]
     parsed-result
     ))
  )

(defn find-concepts
  ([id preferred-label type deprecated offset limit version]
   (find-concepts-by-db id preferred-label type deprecated offset limit (get-db version))
   )
  )

;;"TODO expose this as a private end point for the editor"
(defn find-concepts-including-unpublished
  ([id preferred-label type deprecated offset limit]
   (find-concepts-by-db id preferred-label type deprecated offset limit (get-db))
   )
  ([id]
   (find-concepts-by-db id nil nil nil nil nil (get-db))
   )
  )

(def replaced-by-concept-schema
  {:id s/Str
   :type s/Str
   :definition s/Str
   :preferredLabel s/Str
   (s/optional-key :deprecated) s/Bool
   }
  )

(def concept-schema
  {:id s/Str
   :type s/Str
   :definition s/Str
   :preferredLabel s/Str
   (s/optional-key :deprecated) s/Bool
   (s/optional-key :replacedBy)  [replaced-by-concept-schema]
   }
  )

(def find-concepts-schema
  "The response schema for /concepts. Beta for v0.9."
  [concept-schema ])

(defn assert-concept-part [type desc preferred-label]
  (let* [new-concept {:concept/id (nano/generate-new-id-with-underscore)
                      :concept/definition desc
                      :concept/type type
                      :concept/preferred-label preferred-label
                      }
         tx        [ new-concept]
         result     (d/transact (get-conn) {:tx-data tx})]
         [result new-concept]))

(defn assert-concept "" [type desc preferrerd-label]
  (let [existing (find-concepts-including-unpublished nil preferrerd-label type nil nil nil)]
    (if (> (count existing) 0)
      [false nil]
      (let [[result new-concept] (assert-concept-part type desc preferrerd-label)
            timestamp (if result (nth (first (:tx-data result)) 2) nil)]
        [result timestamp (api-util/rename-concept-keys-for-api new-concept)]))))
