(ns jobtech-taxonomy-api.db.api-util
  (:require
   [clojure.set :as set]
   )
  )

(defn rename-concept-keys-for-api [concept]
  (set/rename-keys concept {:concept/preferred-label :preferredLabel, :concept/id :id, :concept/definition :definition, :concept/type :type :concept/deprecated :deprecated}))


(defn parse-find-concept-datomic-result [result]
  (->> result
       (map first)
       (map rename-concept-keys-for-api)
       )
  )

(defn pagination
  [coll offset limit]
  (cond
    (and coll offset limit) (take limit (drop offset coll))
    (and coll limit) (take limit coll)
    (and coll offset) (drop offset coll)
    :else coll
    )
  )
