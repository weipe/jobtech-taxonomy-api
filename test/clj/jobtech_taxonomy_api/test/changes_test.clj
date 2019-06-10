(ns jobtech-taxonomy-api.test.changes-test
  (:require [clojure.test :as test]
            [jobtech-taxonomy-api.test.test-utils :as util]
            [jobtech-taxonomy-api.db.events :as events]
            [jobtech-taxonomy-api.db.core :as core]
            ))

(test/use-fixtures :each util/fixture)

(test/deftest ^:integration changes-test-0
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


(test/deftest ^:integration-inactive changes-test-1
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn my-filtering-created-function [element]

  ( = "CREATED" (:eventType element))

  )

(defn my-filtering-transactionid-function [element]

  ( = "13194139533328" (:transactionId element))

  )

(defn my-filtering-deprecated-function [element]

  ( = "DEPRECATED" (:eventType element))
  )

(test/deftest ^:integration-inactive changes-test-2
  (test/testing "test event stream created"
    (let [[status body] (util/send-request-to-json-service
                          :get "/v0/taxonomy/public/changes"
                          :headers [util/header-auth-user]
                          :query-params [{:key "fromDateTime", :val "2018-05-21%2009%3A46%3A08"}])
          ]
      ( test/is (not (empty? (filter my-filtering-created-function body))))
      )))




(test/deftest ^:integration-inactive changes-test-3
  (test/testing "test event stream deprecated"
    (let [[status body] (util/send-request-to-json-service
                          :get "/v0/taxonomy/public/changes"
                          :headers [util/header-auth-user]
                          :query-params [{:key "fromDateTime", :val "2018-05-21%2009%3A46%3A08"}])
         ]
      ( test/is (not (empty? (filter my-filtering-deprecated-function body))))
      )))

(test/deftest ^:integration-inactive changes-test-4
  (test/testing "test event stream transactionid"
    (let [[status body] (util/send-request-to-json-service
                          :get "/v0/taxonomy/public/changes"
                          :headers [util/header-auth-user]
                          :query-params [{:key "fromDateTime", :val "2018-05-21%2009%3A46%3A08"}])
          ]
      ( test/is  (empty? (filter my-filtering-transactionid-function body)))
      )))
