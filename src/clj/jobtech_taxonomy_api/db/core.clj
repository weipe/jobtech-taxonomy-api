(ns jobtech-taxonomy-api.db.core
  (:require
   [datomic.client.api :as d]
   [schema.core :as s]
   [clojure.test :refer [is]]
   [clj-time.coerce :as c]
   [clj-time [format :as f]]
   [clojure.set :as set]
   [mount.core :refer [defstate]]
   [jobtech-taxonomy-api.config :refer [env]]
   [jobtech-taxonomy-database.nano-id :as nano]
   [jobtech-taxonomy-api.db.events :refer :all]
   [jobtech-taxonomy-api.db.database-connection  :refer :all]
   [jobtech-taxonomy-api.db.api-util :refer :all]
   [jobtech-taxonomy-api.db.concepts :as db-concepts]
   ))


(defn retract-concept [id]
  (let [found-concept (db-concepts/find-concepts id)]
    (if (or (= 0 (count found-concept))
            (get (ffirst found-concept) :concept/deprecated))
      false
      (and (d/transact (get-conn) {:tx-data
                                   [{:concept/id id
                                     :concept/deprecated true}]})
           true))))


(def get-relation-graph-query
  '[:find ?c1id ?c2id
    :in $ ?relation-type
    :where
    [?re :relation/type      ?relation-type]
    [?re :relation/concept-1 ?c1]
    [?re :relation/concept-2 ?c2]
    [?c1 :concept/id ?c1id]
    [?c2 :concept/id ?c2id]])

(def get-relation-graph-from-concept-query-rules
  '[[(related-concepts ?c1 ?c2 ?t)
     [?re :relation/type      ?t]
     [?re :relation/concept-1 ?c1]
     [?re :relation/concept-2 ?c2]]])

(def get-relation-graph-from-concept-query
  '[:find (pull ?child [:db/id
                        :concept/id
                        :concept/preferred-label])
    :in $ % ?relation-type ?id :where
    [?parent :concept/id ?id]
    (related-concepts ?parent ?child ?relation-type)])

(def map-id-to-term-query
  '[:find ?preferred-label
    :in $ ?id
    :where
    [?ce :concept/id ?id]
    [?ce :concept/preferred-label ?preferred-label]
    ])

; (find-concept-by-id "aqxj_t1i_SxL") ; Sydsudan
; (find-concept-by-id "Gk4Z_5LP_v5G") ; Nordafrika

(defn map-id-to-term [id]
  "Return the preferred label for a given ID. The query returns a list of lists, therefore we do an (ffirst)."
  (ffirst (d/q map-id-to-term-query (get-db) id)))

(defn get-relation-graph [relation-type]
  (letfn [(lazy-contains? [col key]
            (some #{key} col))
          (get-children [n all-pairs]
            (flatten (map vals (filter #(contains? % n) all-pairs))))
          (get-hier-rec [parent all-pairs]
            (let [children (get-children parent all-pairs)
                  term (map-id-to-term parent)]
              (if (empty? children)
                { "name" term, "id" parent }
                { "name" term, "id" parent, "children" (map #(get-hier-rec % all-pairs) children) })))]
    (let [list (d/q get-relation-graph-query (get-db) relation-type)
          map-key-val (map #(apply hash-map % ) list)
          map-val-key (map #(apply hash-map (reverse %) ) list)
          tops (->> (map #(first (keys %)) map-key-val)
                    (filter #(not (lazy-contains? map-val-key %)) ,,,)
                    (distinct ,,,))]
      { "name" "Jobtech", "id" "666", "children" (map #(get-hier-rec % map-key-val) tops) })))

(defn get-relation-graph-from-concept [relation-type id]
  (letfn [(format-answer [[list]]
            { "name" (get-in list [:concept/preferred-label]) "id" (get-in list [:concept/id])})]
    (let [lookup (d/q get-relation-graph-from-concept-query (get-db) get-relation-graph-from-concept-query-rules relation-type id)]
      { "name" (map-id-to-term id), "id" id, "children" (map #(format-answer %) lookup) })))

(def get-relation-types-query
  '[:find ?v :where [_ :relation/type ?v]])


(defn get-relation-types []
  (->> (d/q get-relation-types-query (get-db))
       (sort-by first)
       (apply concat)))

;; TODO appeda pa replaced by listan


(defn replace-deprecated-concept [old-concept-id new-concept-id]
  (let [data {:concept/id old-concept-id
              :concept/replaced-by [{:concept/id new-concept-id}]}
        result (d/transact (get-conn) {:tx-data [data]})
        timestamp (nth (first (:tx-data result)) 2)]

    {:msg (if result {:timestamp timestamp :status "OK"} {:status "ERROR"})}))


(def get-all-taxonomy-types-query
  '[:find ?v :where [_ :concept/type ?v]])

(defn get-all-taxonomy-types "Return a list of taxonomy types." []
  (->> (d/q get-all-taxonomy-types-query (get-db))
       (sort-by first)
       (flatten)
       (map name)))


(defn show-deprecated-concepts-and-replaced-by [date-time]
  (get-deprecated-concepts-replaced-by-since (get-db) date-time))
