(ns jobtech-taxonomy-api.test.changes-test
  (:require [clojure.test :as test]
            [jobtech-taxonomy-api.test.test-utils :as util]
            [jobtech-taxonomy-api.db.events :as events]
            [jobtech-taxonomy-api.db.core :as core]
            ))

(test/use-fixtures :each util/fixture)

(test/deftest ^:integration-changes-test-0 changes-test-0
  (test/testing "test event stream"
    (core/assert-concept "skill" "cykla" "cykla")
    (let [[status body] (util/send-request-to-json-service
                         :get "/v0/taxonomy/public/changes"
                         :headers [util/header-auth-user]
                         :query-params [{:key "fromDateTime", :val "2019-05-21%2009%3A46%3A08"}])
          an-event (first body)
          found-concept (first (core/find-concept-by-preferred-term "cykla"))]

      (test/is (= "CREATED" (:eventType an-event)))

      (test/is (= "cykla" (get found-concept :preferredLabel))))))


(test/deftest ^:integration-changes-test-1 changes-test-1
  (test/testing "test event stream"
    (core/assert-concept "skill2" "cykla2" "cykla2")
    (let [[status body] (util/send-request-to-json-service
                         :get "/v0/taxonomy/public/changes"
                         :headers [util/header-auth-user]
                         :query-params [{:key "fromDateTime", :val "2019-05-21%2009%3A46%3A08"}])
          an-event (first body)
          found-concept (first (core/find-concept-by-preferred-term "cykla2"))]

      (test/is (= "CREATED" (:eventType an-event)))

      (test/is (= "cykla2" (get found-concept :preferredLabel))))))

