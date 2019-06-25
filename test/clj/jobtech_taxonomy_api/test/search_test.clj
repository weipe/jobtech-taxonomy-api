(ns jobtech-taxonomy-api.test.search-test
  (:require [clojure.test :as test]
            [jobtech-taxonomy-api.test.test-utils :as util]
            [jobtech-taxonomy-api.db.events :as events]
            [jobtech-taxonomy-api.db.concepts :as concept]
            [jobtech-taxonomy-api.db.core :as core]))

(test/use-fixtures :each util/fixture)



(test/deftest ^:integration-search-test-0 search-test-0
  (test/testing "test search "
    (concept/assert-concept "skill" "cyklade" "cykla")
    (let [[status body] (util/send-request-to-json-service
                          :get "/v0/taxonomy/public/search"
                          :headers [util/header-auth-user]
                          :query-params [{:key "q", :val "cykla"}])
          found-concept (first (core/find-concept-by-preferred-term "cykla"))]
      (test/is (= "cykla" (get found-concept :preferredLabel))))))
