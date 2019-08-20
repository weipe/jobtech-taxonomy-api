(ns jobtech-taxonomy-api.test.versions-test
  (:require [clojure.test :as test]
            [datomic.client.api :as d]
            [jobtech-taxonomy-api.test.test-utils :as util]
            [jobtech-taxonomy-api.db.events :as events]
            [jobtech-taxonomy-api.db.concepts :as concept]
            [jobtech-taxonomy-api.db.versions :as versions]
            [jobtech-taxonomy-api.db.database-connection :refer :all]
            [jobtech-taxonomy-api.db.core :as core])
  )
;; This namespace is for testing versions


;; create test data in database / setup
(defn write-test-data-to-database! []
  (do
    (d/transact (get-conn) {:tx-data [ {:taxonomy-version/id 66}]})
    (concept/assert-concept "skill" "Gammel Java, the old version" "Gammel Java")
    (versions/create-new-version 67)
    (let [concept-id (:id (first (concept/find-concepts nil "Gammel Java" "skill" nil nil nil nil)))]
      (core/retract-concept concept-id)
      )
    (versions/create-new-version 68)
    (concept/assert-concept "skill" "Ny Java the new version of java" "Ny Java")
    )
  )
