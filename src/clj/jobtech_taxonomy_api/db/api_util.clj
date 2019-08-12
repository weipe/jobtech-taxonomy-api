(ns jobtech-taxonomy-api.db.api-util
  (:require
   [clojure.set :as set]
   )
  )

(defn transform-replaced-by [concept]
  (set/rename-keys concept {:concept/id :id
                        :concept/definition :definition
                        :concept/type :type
                        :concept/preferred-label :preferredLabel
                        :concept/deprecated :deprecated })
 )


(defn rename-concept-keys-for-api [concept]
  (let [renamed-concept (set/rename-keys concept {:concept/preferred-label :preferredLabel, :concept/id :id, :concept/definition :definition, :concept/type :type :concept/deprecated :deprecated :concept/replaced-by :replacedBy})]

    (if (:replacedBy renamed-concept)
      (update renamed-concept :replacedBy #(map transform-replaced-by %))
      renamed-concept
      )
    )
  )


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
