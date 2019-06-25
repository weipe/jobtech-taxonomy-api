(ns jobtech-taxonomy-api.test.events-test
  (:require [jobtech-taxonomy-api.db.events :refer :all]
            [clojure.test :as test]
            )
  )


(def datoms-test-data
  [[27874818787377248
    :concept/type
    "skill"
    13194139533318
    true
    #inst "2019-06-24T12:27:27.806-00:00"
    "yLeW_gBx_UHv"
    "java"
    "skill"]
   [27874818787377248
    :concept/preferred-label
    "Java"
    13194139533319
    true
    #inst "2019-06-24T12:37:36.766-00:00"
    "yLeW_gBx_UHv"
    "java"
    "skill"]
   [27874818787377248
    :concept/preferred-label
    "Java"
    13194139533319
    true
    #inst "2019-06-24T12:37:36.766-00:00"
    "yLeW_gBx_UHv"
    "Java"
    "skill"]
   [27874818787377248
    :concept/preferred-label
    "java"
    13194139533319
    false
    #inst "2019-06-24T12:37:36.766-00:00"
    "yLeW_gBx_UHv"
    "java"
    "skill"]
   [27874818787377248
    :concept/id
    "yLeW_gBx_UHv"
    13194139533318
    true
    #inst "2019-06-24T12:27:27.806-00:00"
    "yLeW_gBx_UHv"
    "Java"
    "skill"]
   [27874818787377248
    :concept/definition
    "java"
    13194139533318
    true
    #inst "2019-06-24T12:27:27.806-00:00"
    "yLeW_gBx_UHv"
    "java"
    "skill"]
   [27874818787377248
    :concept/type
    "skill"
    13194139533318
    true
    #inst "2019-06-24T12:27:27.806-00:00"
    "yLeW_gBx_UHv"
    "Java"
    "skill"]
   [34317956926144609
    :concept/definition
    "Clojure funky programming"
    13194139533321
    true
    #inst "2019-06-24T13:57:40.040-00:00"
    "r7yE_Xe9_dcT"
    "clojure"
    "skill"]
   [27874818787377248
    :concept/definition
    "java"
    13194139533318
    true
    #inst "2019-06-24T12:27:27.806-00:00"
    "yLeW_gBx_UHv"
    "Java"
    "skill"]
   [27874818787377248
    :concept/deprecated
    true
    13194139533320
    true
    #inst "2019-06-24T12:38:56.876-00:00"
    "yLeW_gBx_UHv"
    "java"
    "skill"]
   [27874818787377248
    :concept/deprecated
    true
    13194139533320
    true
    #inst "2019-06-24T12:38:56.876-00:00"
    "yLeW_gBx_UHv"
    "Java"
    "skill"]
   [34317956926144609
    :concept/type
    "skill"
    13194139533321
    true
    #inst "2019-06-24T13:57:40.040-00:00"
    "r7yE_Xe9_dcT"
    "clojure"
    "skill"]
   [34317956926144609
    :concept/id
    "r7yE_Xe9_dcT"
    13194139533321
    true
    #inst "2019-06-24T13:57:40.040-00:00"
    "r7yE_Xe9_dcT"
    "clojure"
    "skill"]
   [27874818787377248
    :concept/preferred-label
    "java"
    13194139533318
    true
    #inst "2019-06-24T12:27:27.806-00:00"
    "yLeW_gBx_UHv"
    "java"
    "skill"]
   [27874818787377248
    :concept/preferred-label
    "java"
    13194139533319
    false
    #inst "2019-06-24T12:37:36.766-00:00"
    "yLeW_gBx_UHv"
    "Java"
    "skill"]
   [27874818787377248
    :concept/id
    "yLeW_gBx_UHv"
    13194139533318
    true
    #inst "2019-06-24T12:27:27.806-00:00"
    "yLeW_gBx_UHv"
    "java"
    "skill"]
   [27874818787377248
    :concept/preferred-label
    "java"
    13194139533318
    true
    #inst "2019-06-24T12:27:27.806-00:00"
    "yLeW_gBx_UHv"
    "Java"
    "skill"]
   [34317956926144609
    :concept/preferred-label
    "clojure"
    13194139533321
    true
    #inst "2019-06-24T13:57:40.040-00:00"
    "r7yE_Xe9_dcT"
    "clojure"
    "skill"]])


(def expected-events '({:event-type "CREATED",
                       :transaction-id 13194139533318,
                       :type "skill",
                       :timestamp #inst "2019-06-24T12:27:27.806-00:00",
                       :concept-id "yLeW_gBx_UHv",
                       :preferred-label "java"}
                      {:event-type "UPDATED",
                       :transaction-id 13194139533319,
                       :type "skill",
                       :timestamp #inst "2019-06-24T12:37:36.766-00:00",
                       :concept-id "yLeW_gBx_UHv",
                       :preferred-label "java"}
                      {:event-type "CREATED",
                       :transaction-id 13194139533321,
                       :type "skill",
                       :timestamp #inst "2019-06-24T13:57:40.040-00:00",
                       :concept-id "r7yE_Xe9_dcT",
                       :preferred-label "clojure"}
                      {:event-type "DEPRECATED",
                       :transaction-id 13194139533320,
                       :type "skill",
                       :timestamp #inst "2019-06-24T12:38:56.876-00:00",
                       :concept-id "yLeW_gBx_UHv",
                       :preferred-label "java",
                       :deprecated true}))

(test/deftest test-convert-history-to-events
  (test/is (= (convert-history-to-events datoms-test-data) expected-events)))
