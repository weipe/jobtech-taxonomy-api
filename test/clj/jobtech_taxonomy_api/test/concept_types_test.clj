(ns jobtech-taxonomy-api.test.concept-types-test
  (:require [clojure.test :as test]
            [schema.core :as s]
            [jobtech-taxonomy-api.test.test-utils :as util]))


(test/use-fixtures :each util/fixture)

(defn member? [list elt]
  "True if list contains at least one instance of elt"
  (cond
    (empty? list) false
    (= (first list) elt) true
    true (recur (rest list) elt)))


(test/deftest ^:integration-concept-types-not-empty concept-types-not-empty
  (test/testing "access to /v0/taxonomy/public/concept/types"
    (let [body ["continent"
                "country"
                "driving-license"
                "employment-duration"
                "employment-type"
                "isco"
                "keyword"
                "language"
                "language-level"
                "municipality"
                "occupation-collection"
                "occupation-field"
                "occupation-group"
                "occupation-name"
                "region"
                "skill"
                "skill-headline"
                "skill-main-headline"
                "ssyk-level-1"
                "ssyk-level-2"
                "ssyk-level-3"
                "sun-education-field-1"
                "sun-education-field-2"
                "sun-education-field-3"
                "sun-education-level-1"
                "sun-education-level-2"
                "sun-education-level-3"
                "wage-type"
                "worktime-extent"]]

      (test/is (member? body "skill")
                        ))))


