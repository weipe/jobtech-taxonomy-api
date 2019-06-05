(ns jobtech-taxonomy-api.test.changes-test
  (:require [clojure.test :as test]
            [jobtech-taxonomy-api.test.test-utils :as util]
            [jobtech-taxonomy-api.db.events :as events]
            ))

(def run-fixture? (atom true))


(defn my-filtering-created-function [element]

  ( = "CREATED" (:eventType element))

  )


(defn my-filtering-transactionid-function [element]

  ( = "13194139533328" (:transactionId element))

  )

(defn my-filtering-deprecated-function [element]

  ( = "DEPRECATED" (:eventType element))
  )

(test/use-fixtures :each (partial util/fixture run-fixture?))

(test/deftest ^:integration changes-test-0
  (test/testing "test event stream created"
    (let [[status body] (util/send-request-to-json-service
                          :get "/v0/taxonomy/public/changes"
                          :headers [util/header-auth-user]
                          :query-params [{:key "fromDateTime", :val "2018-05-21%2009%3A46%3A08"}])
          ]
      ( test/is (not (empty? (filter my-filtering-created-function body))))
      )))




(test/deftest ^:integration changes-test-1
  (test/testing "test event stream deprecated"
    (let [[status body] (util/send-request-to-json-service
                          :get "/v0/taxonomy/public/changes"
                          :headers [util/header-auth-user]
                          :query-params [{:key "fromDateTime", :val "2018-05-21%2009%3A46%3A08"}])
         ]
      ( test/is (not (empty? (filter my-filtering-deprecated-function body))))
      )))

(test/deftest ^:integration changes-test-2
  (test/testing "test event stream transactionid"
    (let [[status body] (util/send-request-to-json-service
                          :get "/v0/taxonomy/public/changes"
                          :headers [util/header-auth-user]
                          :query-params [{:key "fromDateTime", :val "2018-05-21%2009%3A46%3A08"}])
          ]
      ( test/is  (empty? (filter my-filtering-transactionid-function body)))
      )))




