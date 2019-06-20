(ns jobtech-taxonomy-api.db.information-extraction
  (:refer-clojure :exclude [type])
  (:require
   [schema.core :as s]
   [datomic.client.api :as d]
   [jobtech-taxonomy-api.db.database-connection :refer :all]
   [jobtech-taxonomy-api.db.api-util :refer :all]
   [clojure.set :as set]
   )
  )

(def get-all-concepts-query
  '[:find ?label ?id
    :in $
    :where
    [?c :concept/id ?id]
    [?c :concept/preferred-label ?label]
    ])

(defn get-all-concepts []
  (d/q get-all-concepts-query (get-db))
  )

(def all-concepts (memoize get-all-concepts))

(defn create-regex-pattern []
  (clojure.string/join "|" (map first (all-concepts)))
  )

(defn build-regex []
  (re-pattern (create-regex-pattern))
  )

(def taxonomy-regex (memoize build-regex))


(defn find-taxonomy-in-text [text]
 (re-find (taxonomy-regex) text)
  )
