(ns jobtech-taxonomy-api.test.information-extraction-test
  (:require [clojure.test :as test]
            [jobtech-taxonomy-api.test.test-utils :as util]
            [jobtech-taxonomy-api.db.information-extraction :as inf-ext]
            [jobtech-taxonomy-api.test.test-utils :as util]
            [jobtech-taxonomy-api.db.concepts :as concept]
            [clojure.pprint :as pp]
            ))

(test/use-fixtures :each util/fixture)

(defn comp
  [el1 el2]
  (compare (:preferredLabel el1) (:preferredLabel el2)))

(test/deftest ^:xyz information-extraction-test0
  (test/testing ""
    (let [[db-before inst ent] (concept/assert-concept
                                "skill" "javaprogrammering" "javaprogrammering")
          new-id (get ent :id)
          analysis (sort comp (inf-ext/parse-text "javaprogrammering"))
          correct (sort comp [{:id new-id, :type "skill", :preferredLabel "programmering"}
                         {:id new-id, :type "skill", :preferredLabel "java"}
                         {:id new-id, :type "skill", :preferredLabel "javaprogrammering"}])]
      (test/is (= analysis correct)))))
