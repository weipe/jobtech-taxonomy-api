(ns jobtech-taxonomy-api.test.changes-test
  (:require [clojure.test :as test]
            [jobtech-taxonomy-api.test.test-utils :as util]))

(def run-fixture? (atom true))

(test/use-fixtures :each (partial util/fixture run-fixture?))

(test/deftest ^:integration changes-test-0
  (test/testing "test event stream"
    (let [[status body] (util/send-request-to-json-service
                         :get "/v0/taxonomy/public/changes"
                         :headers [util/header-auth-user]
                         :query-params [{:key "fromDateTime", :val "2019-05-21%2009%3A46%3A08"}])
          an-event (first body)]
      ;; (prn an-event)
      (test/is (= "DEPRECATED" (:eventType an-event))))))
