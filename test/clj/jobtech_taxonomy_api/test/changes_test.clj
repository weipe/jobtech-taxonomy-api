(ns jobtech-taxonomy-api.test.changes-test
  (:require [clojure.test :as test]
            [jobtech-taxonomy-api.test.test-utils :as util]
            [jobtech-taxonomy-api.db.events :as events]
            ))

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



(def datoms-update-preferred-term
   [[7327145487499336
    :concept/preferred-term
    70746976177619018
    13194139533320
    true
    #inst "2019-02-16T17:10:51.894-00:00"
    "4"
    "Annonsassistent"
    32136525856637001]
   [7327145487499336
    :concept/preferred-term
    32136525856637001
    13194139533320
    false
    #inst "2019-02-16T17:10:51.894-00:00"
    "4"
    "Annonsassistent"
    32136525856637001]
   [7327145487499336
    :concept/preferred-term
    70746976177619018
    13194139533320
    true
    #inst "2019-02-16T17:10:51.894-00:00"
    "4"
    "Annonsassistent/Annonssekreterare"
    70746976177619018]
   [7327145487499336
    :concept/preferred-term
    32136525856637001
    13194139533320
    false
    #inst "2019-02-16T17:10:51.894-00:00"
    "4"
    "Annonsassistent/Annonssekreterare"
    70746976177619018]]
  )

(def expected-update-events
  '({:event-type "UPDATED",
     :transaction-id 13194139533320,
     :category nil,
     :timestamp #inst "2019-02-16T17:10:51.894-00:00",
     :concept-id "4",
     :preferred-term "Annonsassistent/Annonssekreterare"})
  )


(test/deftest  changes-test-update-event
  (test/testing "Testing convert datoms to update event"
    (test/is (= expected-update-events
                (events/convert-history-to-events datoms-update-preferred-term)
                ))))
