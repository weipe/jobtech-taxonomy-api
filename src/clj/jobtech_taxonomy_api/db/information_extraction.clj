(ns jobtech-taxonomy-api.db.information-extraction
  (:refer-clojure :exclude [type])
  (:require
   [schema.core :as s]
   [datahike.api :as d]
   [jobtech-taxonomy-api.db.database-connection :refer :all]
   [jobtech-taxonomy-api.db.api-util :refer :all]
   [clojure.set :as set]
   )
  )

(def get-all-concepts-query
  '[:find ?label ?id ?type
    :in $
    :where
    [?c :concept/id ?id]
    [?c :concept/preferred-label ?label]
    [?c :concept/type ?type]
    ])

(defn- get-all-concepts []
  (d/q get-all-concepts-query (get-db))
  )

(def all-concepts (memoize get-all-concepts))

(defn- create-regex-pattern [words]
  (str "(?i)(" (clojure.string/join "|" (map #(str "\\b" %  "\\b") words)) ")" )
  )

(defn- build-regex []
  (re-pattern (create-regex-pattern (map first (all-concepts)) ))
  )

(def taxonomy-regex (memoize build-regex))


(defn- to-concept [[label id type]]
  {:id id
   :type type
   :preferredLabel label
   }
  )

(defn- dictionary-reducer-fn [acc tuple]
  (update acc (clojure.string/lower-case (first tuple)) conj (to-concept tuple) )
  )

(defn- build-dictionary []
  "Creates a dictionary with the token to look for as a key and the concept as a value."
  (reduce dictionary-reducer-fn {} (all-concepts))
  )

(def taxonomy-dictionary (memoize build-dictionary))

(defn- lookup-in-taxonomy-dictionary [word]
  (get (taxonomy-dictionary) (clojure.string/lower-case word))
  )

(defn parse-text [text]
  (let [matches (map first (re-seq (taxonomy-regex) text))
        concepts (seq (set (mapcat lookup-in-taxonomy-dictionary matches)))
        ]
    concepts
    )
  )
