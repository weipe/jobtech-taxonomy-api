(ns jobtech-taxonomy-api.db.api-util
  (:require
   [clojure.set :as set]
   )
  )

(defn rename-concept-keys-for-api [concept]
  (set/rename-keys concept {:concept/preferred-label :preferredLabel, :concept/id :id, :concept/definition :definition, :concept/type :type :concept/deprecated :deprecated}))

(defn lift-term [concept]
  (assoc (dissoc concept :preferred-term)
         :concept/preferred-term (get-in concept [:concept/preferred-term :term/base-form] )))

(defn parse-find-concept-datomic-result [result]
  (->> result
       (map first)
       (map rename-concept-keys-for-api)
       )
  )



(defmacro pagination
  "Pagination mimicking the MySql LIMIT"
  ([coll start-from quantity]
   `(take ~quantity (drop ~start-from ~coll)))
  ([coll quantity]
   `(pagination ~coll 0 ~quantity)))


(defn empty-string-to-nil [string]
  (if (seq string)
    string
    nil
    )
  )

(defn paginate-datomic-result [result offset limit]
  (cond
    (and (= 0 offset) (= 0 limit)) (pagination result 0 100)
    (and offset limit) (pagination result offset limit)
    offset (drop offset result)
    limit (take limit result)
    :else (pagination result 0 100)
    )
  )
