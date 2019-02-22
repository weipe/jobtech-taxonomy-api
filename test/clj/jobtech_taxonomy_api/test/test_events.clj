(ns jobtech-taxonomy-api.test-events
  (:require
   [datomic.client.api :as d]
   [mount.core :refer [defstate]]
   [jobtech-taxonomy-api.db.events :refer :all]
   )
  )

(def cfg { :datomic-name "taxonomy_v13"
          :datomic-cfg {
                        :server-type :ion
                        :region "eu-west-1" ;; e.g. us-east-1
                        :system "prod-jobtech-taxonomy-db"
                        ;;:creds-profile "<your_aws_profile_if_not_using_the_default>"
                        :endpoint "http://entry.prod-jobtech-taxonomy-db.eu-west-1.datomic.net:8182/"
                        :proxy-port 8182}

          })


(defn get-client [] (d/client (:datomic-cfg cfg)))


(def  conn
  (d/connect (get-client)  {:db-name "taxonomy_v13"} )
  )

(defn get-conn []
  (d/connect (get-client)  {:db-name "taxonomy_v13"} )
  )

(defn get-db [] (d/db conn))


(defn test-get-deprecated-concepts-sine [date-time]

  (get-deprecated-concepts-replaced-by-since (get-db) #inst "2019-02-10")
  )
