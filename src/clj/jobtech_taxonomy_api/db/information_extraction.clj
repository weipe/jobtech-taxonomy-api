(ns jobtech-taxonomy-api.db.information-extraction
  (:refer-clojure :exclude [type])
  (:require
   [schema.core :as s]
   [clojure.java.io :as io]
   [datomic.client.api :as d]
   [jobtech-taxonomy-api.db.database-connection :refer :all]
   [jobtech-taxonomy-api.db.api-util :refer :all]
   [clojure.set :as set]
   [clojure.string :as str]
   [jobtech-nlp-tokeniser.tokeniser :as tokeniser]
   [jobtech-nlp-stop-words.stop-words :as stop-words]
   [nlp.compound-splitter.stava :as stava]
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

(defn dumb-split [word]
  (first (stava/split word)))

(defn- tokenise [concepts]
  (mapcat (fn [[term id typ]]
            (let [space-split (tokeniser/tokenise-no-punctuation term)
                  comp-split (flatten (map #(flatten (dumb-split %)) space-split))
                  joined (remove stop-words/stop-word?
                                 (remove str/blank?
                                         (distinct
                                          (map str/lower-case ;; lower-case here may be questionable decision
                                               (concat space-split comp-split)))))] ;;FIXME: this mess was made in a hurry - improve
              (map #(list % term id typ) (flatten joined))))
          concepts))

(defn- get-all-concepts []
  (let [all-concepts
        (d/q get-all-concepts-query (get-db))]
    (tokenise all-concepts)))

(def all-concepts (memoize get-all-concepts))

(defn- create-regex-pattern [words]
  (str "(?i)(" (clojure.string/join "|" (map #(str "\\b" %  "\\b") words)) ")" )
  )

(defn- build-regex []
  (re-pattern (create-regex-pattern (map first (all-concepts)) ))
  )

(def taxonomy-regex (memoize build-regex))


(defn- to-concept [[_ label id type]]
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

(defn tokenise-and-compound-split [text]
  (flatten (map (fn [word]
                  (remove stop-words/stop-word? (remove #(empty? %) (dumb-split word))))
                (tokeniser/tokenise-no-punctuation text))))

(defn parse-text-experiment-with-text-compound-splitting [text]
  (let [full-matches (map first (re-seq (taxonomy-regex) text))
        compound-text (str/join " " (tokenise-and-compound-split text))
        compound-matches (map first (re-seq (taxonomy-regex) compound-text))
        all-matches (distinct (concat full-matches compound-matches))
        concepts (seq (set (mapcat lookup-in-taxonomy-dictionary all-matches)))
        ]
    (distinct concepts)))

(defn parse-text [text]
  (let [full-matches (map first (re-seq (taxonomy-regex) text))
        concepts (seq (set (mapcat lookup-in-taxonomy-dictionary full-matches)))]
    (distinct concepts)))

;; (parse-text "restaurang, språk")
;; (dumb-split "Cobolprogrammering")
;; (str/join " " (tokenise-and-compound-split "Javaprogrammerare"))
;; (str/join " " (dumb-split "båtmotor"))
;; (parse-text "jag kan javaprogrammering och C, och är en Cobolprogrammerare")
