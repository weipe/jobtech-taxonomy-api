(ns jobtech-taxonomy-api.test.concepts-test
  (:require [clojure.test :as test]
            [jobtech-taxonomy-api.test.test-utils :as util]
            [jobtech-taxonomy-api.db.events :as events]
            [jobtech-taxonomy-api.db.core :as core]
            ))

(test/use-fixtures :each util/fixture)

(test/deftest ^:integration-concepts-test-0 concepts-test-0
  (test/testing "test concepts "
    (core/assert-concept "skill" "cyklade" "cykla")
    (let [[status body] (util/send-request-to-json-service
                          :get "/v0/taxonomy/public/concepts"
                          :headers [util/header-auth-user]
                          :query-params [{:key "type", :val "skill"}])
          found-concept (first (core/find-concept-by-preferred-term "cykla"))]
      (test/is (= "cykla" (get found-concept :preferredLabel))))))


(test/deftest ^:integration-concepts-test-1 concepts-test-1
  (test/testing "test concepts"
    (core/assert-concept "skill2" "cyklade" "cykla2")
    (let [[status body] (util/send-request-to-json-service
                          :get "/v0/taxonomy/public/concepts"
                          :headers [util/header-auth-user]
                          :query-params [{:key "type", :val "skill"}])
          found-concept (first (core/find-concept-by-preferred-term "cykla2"))]
      (test/is (= "cykla2" (get found-concept :preferredLabel))))))