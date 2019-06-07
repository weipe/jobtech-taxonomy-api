(ns jobtech-taxonomy-api.test.changes-test
  (:require [clojure.test :as test]
            [jobtech-taxonomy-api.test.test-utils :as util]
            [jobtech-taxonomy-api.db.events :as events]
            [jobtech-taxonomy-api.db.core :as core]
            ))

;;(def test-db-name "Two tests cannot run simultanously, so generate unique names."
;;  (str "integration-test-" (rand-int Integer/MAX_VALUE)))
;;
;;(test/use-fixtures :each (partial util/fixture test-db-name))
;;

(test/use-fixtures :each util/fixture)

;;  (let [some-terms        [{:term/base-form "Kontaktmannaskap"}
;;                           {:term/base-form "Fribrottare"}
;;                           {:term/base-form "Begravningsentreprenör"}]
;;
;;        some-concepts     [{:concept/id "MZ6wMoAfyP"
;;                            :concept/description "grotz"
;;                            :concept/category :skill
;;                            :concept/preferred-term [:term/base-form "Kontaktmannaskap"]
;;                            :concept/alternative-terms #{[:term/base-form "Kontaktmannaskap"]}}
;;                           {:concept/id "XYZYXYZYXYZ"
;;                            :concept/description "Fribrottare"
;;                            :concept/category :occupation
;;                            :concept/preferred-term [:term/base-form "Fribrottare"]
;;                            :concept/alternative-terms #{[:term/base-form "Fribrottare"]}}
;;                           {:concept/id "ZZZZZZZZZZZ"
;;                            :concept/description "Begravningsentreprenör"
;;                            :concept/category :occupation
;;                            :concept/preferred-term [:term/base-form "Begravningsentreprenör"]
;;                            :concept/alternative-terms #{[:term/base-form "Begravningsentreprenör"]}}]]
;;    (d/transact (get-conn) {:tx-data (vec (concat some-terms))})
;;    (d/transact (get-conn) {:tx-data (vec (concat some-concepts))}))
;;


;;   (defn xx []
;;     (core/assert-concept "skill" "cykla" "cykla")
;;     (let [[status body] (util/send-request-to-json-service
;;                          :get "/v0/taxonomy/public/changes"
;;                          :headers [util/header-auth-user]
;;                          :query-params [{:key "fromDateTime", :val "2019-05-21%2009%3A46%3A08"}])
;;           an-event (first body)]
;;
;;       body))
;;   (util/fixture xx)
;;

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


(test/deftest ^:integration-inactive changes-test-2
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
