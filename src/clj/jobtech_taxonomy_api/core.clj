(ns jobtech-taxonomy-api.core
  (:require [jobtech-taxonomy-api.handler :as handler]
            [jobtech-taxonomy-api.nrepl :as nrepl]
            [luminus.http-server :as http]
            [jobtech-taxonomy-api.config :refer [env]]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [mount.core :as mount]

            ;; interim deps:
            [datahike.api :as d])
  (:gen-class))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]])

(mount/defstate ^{:on-reload :noop} http-server
  :start
  (http/start
   (-> env
       (assoc  :handler #'handler/app)
       (update :io-threads #(or % (* 2 (.availableProcessors (Runtime/getRuntime)))))
       (update :port #(or (-> env :options :port) %))))
  :stop
  (http/stop http-server))

(mount/defstate ^{:on-reload :noop} repl-server
  :start
  (when (env :nrepl-port)
    (nrepl/start {:bind (env :nrepl-bind)
                  :port (env :nrepl-port)}))
  :stop
  (when repl-server
    (nrepl/stop repl-server)))

(defn stop-app []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn start-app [args]
  (doseq [component (-> args
                        (parse-opts cli-options)
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn interim-db-setup []
    (def schema
    [{:db/ident       :concept/id
      :db/valueType   :db.type/string
      :db/cardinality :db.cardinality/one
      :db/unique      :db.unique/identity
      :db/doc         "Unique identifier for concepts"}

     {:db/ident       :concept/description
      :db/valueType   :db.type/string
      :db/cardinality :db.cardinality/one
      :db/doc         "Text describing the concept, is used for disambiguation. Deprecated"}

     {:db/ident       :concept/definition
      :db/valueType   :db.type/string
      :db/cardinality :db.cardinality/one
      :db/doc         "Text defining the concept, is used for disambiguation."}

     {:db/ident       :concept/preferred-label
      :db/valueType   :db.type/string
      :db/cardinality :db.cardinality/one
      :db/doc         "What we prefer to call the concept."}

   ;deprecated
     {:db/ident       :concept/preferred-term
      :db/valueType   :db.type/ref
      :db/cardinality :db.cardinality/one
      :db/doc         "What we prefer to call the concept"}

     {:db/ident       :concept/alternative-terms
      :db/cardinality :db.cardinality/many
      :db/valueType   :db.type/ref
      :db/doc         "All terms referring to this concept"}

     {:db/ident       :concept/category
      :db/valueType   :db.type/keyword
      :db/cardinality :db.cardinality/one
      :db/doc         "JobTech categories"}

     {:db/ident       :concept/type
      :db/valueType   :db.type/string
      :db/cardinality :db.cardinality/one
      :db/doc         "The concepts main type"}

     {:db/ident       :concept/deprecated
      :db/valueType   :db.type/boolean
      :db/cardinality :db.cardinality/one
      :db/doc         "If a concept is deprecated"}

     {:db/ident       :concept/replaced-by
      :db/valueType   :db.type/ref
      :db/cardinality :db.cardinality/many
      :db/doc         "Refers to other concepts that is replacing this one"}

     {:db/ident       :concept.relation/related
      :db/valueType   :db.type/ref
      :db/cardinality :db.cardinality/many
      :db/doc         "related concepts"}
     {:db/ident       :concept.external-standard/ssyk-2012
      :db/valueType   :db.type/string
      :db/cardinality :db.cardinality/one
      :db/unique      :db.unique/identity
      :db/doc         "SSYK-2012 type"}

     {:db/ident       :concept.category/sort-order
      :db/valueType   :db.type/long
      :db/cardinality :db.cardinality/one
      :db/doc         "Value for display sort order in category"}

     {:db/ident       :concept.external-standard/eures-code
      :db/valueType   :db.type/string
      :db/cardinality :db.cardinality/one
      :db/doc         "EURES code"}

     {:db/ident       :concept.external-standard/driving-licence-code
      :db/valueType   :db.type/string
      :db/cardinality :db.cardinality/one
      :db/doc         "Driving licence code"}

     {:db/ident       :concept.implicit-driving-licences
      :db/valueType   :db.type/ref
      :db/cardinality :db.cardinality/many
      :db/doc         "List of 'lower' ranking driving licences included in the licence"}

     {:db/ident       :concept.external-standard/nuts-level-3-code
      :db/valueType   :db.type/string
      :db/cardinality :db.cardinality/one
      :db/doc         "NUTS level 3 code"}

     {:db/ident       :concept.external-standard/country-code
      :db/valueType   :db.type/string
      :db/cardinality :db.cardinality/one
      :db/doc         "Country code"}

     {:db/ident       :concept.external-database.ams-taxonomy-67/id
      :db/valueType   :db.type/string
      :db/cardinality :db.cardinality/one
      :db/doc         "ID from legacy Taxonomy version 67"}

     {:db/ident       :concept.external-standard/isco-08
      :db/valueType   :db.type/string
      :db/cardinality :db.cardinality/one
      :db/doc         "ISCO-08 level 4"}

     {:db/ident       :concept.external-standard/SUN-field-code
      :db/valueType   :db.type/string
      :db/cardinality :db.cardinality/one
      :db/doc         "SUN education field code, either 1, 2 or 3 digits"}

     {:db/ident       :concept.external-standard/SUN-level-code
      :db/valueType   :db.type/string
      :db/cardinality :db.cardinality/one
      :db/doc         "SUN education level code, either 1, 2 or 3 digits"}

     {:db/ident       :concept.external-standard/sni-level-code
      :db/valueType   :db.type/string
      :db/cardinality :db.cardinality/one
      :db/doc         "SNI level code"}
     {:db/ident       :term/base-form
      :db/valueType   :db.type/string
      :db/cardinality :db.cardinality/one
      :db/unique      :db.unique/identity ; Should this really be unique/identity? Same term can be different concepts. /Sara
      :db/doc         "Term value, the actual text string that is referring to concepts"}

     {:db/ident       :term/special-usage
      :db/valueType   :db.type/keyword
      :db/cardinality :db.cardinality/one
      :db/doc         "A restricted term has term/special-usage :restricted, a historic term has term/special-usage :historic"}

     {:db/ident       :term/term-to-use-instead
      :db/valueType   :db.type/ref
      :db/cardinality :db.cardinality/one
      :db/doc         "A historic term refers to a term to use instead."}
     {:db/ident       :relation/concept-1
      :db/valueType   :db.type/ref
      :db/cardinality :db.cardinality/one
      :db/doc         "The entity ID of the first concept in a relation"}

     {:db/ident       :relation/concept-2
      :db/valueType   :db.type/ref
      :db/cardinality :db.cardinality/one
      :db/doc         "The entity ID of the second concept in a relation"}

     {:db/ident       :relation/type
      :db/valueType   :db.type/string      ;; BREAKING CHANGE!!
      :db/cardinality :db.cardinality/one
      :db/doc         "the type of relationship"}

     {:db/ident       :relation/description
      :db/valueType   :db.type/string
      :db/cardinality :db.cardinality/one
      :db/doc         "Text describing the relation."}

     {:db/ident        :relation/affinity-percentage
      :db/valueType    :db.type/long
      :db/cardinality  :db.cardinality/one
      :db/doc          "The affinity percentage, how well the demand for an occupation is satisfied by a similar occupation"}
     {:db/ident         :taxonomy-version/id
      :db/valueType     :db.type/long
      :db/cardinality   :db.cardinality/one
      :db/unique        :db.unique/identity
      :db/doc           "The current version of the database. Is used almost like a tag in Git."}
     {:db/ident         :scheme/name
      :db/valueType     :db.type/string
      :db/cardinality   :db.cardinality/one
      :db/doc           "The scheme name."}

     {:db/ident        :scheme/member
      :db/valueType    :db.type/ref
      :db/cardinality  :db.cardinality/many
      :db/doc          "members of the scheme"}])

  (def uri "datahike:mem://jobtech-v13")

  (d/delete-database uri)

  (d/create-database uri)

  (d/transact (jobtech-taxonomy-api.db.database-connection/get-conn) schema)

  (d/transact (jobtech-taxonomy-api.db.database-connection/get-conn) {:tx-data [{:taxonomy-version/id 66}]})

  (def api-key "2f904e245c1f5"))

(defn -main [& args]
  (interim-db-setup)
  (start-app args))

(comment

  (start-app)

  )
