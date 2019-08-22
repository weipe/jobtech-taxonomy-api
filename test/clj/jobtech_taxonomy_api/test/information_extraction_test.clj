(ns jobtech-taxonomy-api.test.information-extraction-test
  (:require [clojure.test :as test]
            [jobtech-taxonomy-api.test.test-utils :as util]
            [jobtech-taxonomy-api.db.information-extraction :as inf-ext]
            [jobtech-taxonomy-api.test.test-utils :as util]
            [jobtech-taxonomy-api.db.concepts :as concept]
            [clojure.pprint :as pp]
            ))

(test/use-fixtures :each util/fixture)

(defn comp-cpt
  [el1 el2]
  (compare (:preferredLabel el1) (:preferredLabel el2)))

(test/deftest ^:xyz information-extraction-test0
  (test/testing ""
    (let [[db-before inst ent] (concept/assert-concept
                                "skill" "javaprogrammering" "javaprogrammering")
          new-id (get ent :id)
          analysis (sort comp-cpt (inf-ext/parse-text "java"))
          correct (sort comp-cpt [{:id new-id, :type "skill", :preferredLabel "javaprogrammering"}])]
      (test/is (= analysis correct)))))

(test/deftest ^:xyz2 information-extraction-test1
  (test/testing "Lagra två koncept 'javaprogrammering' och 'kodprogrammering', och kolla att söktermen 'programmering' returnerar båda två."
    (let [[_ _ ent-javaprogrammering] (concept/assert-concept
                                       "skill" "javaprogrammering" "javaprogrammering")
          [_ _ ent-kodprogrammering] (concept/assert-concept
                                       "skill" "kodprogrammering" "kodprogrammering")
          new-id-javaprogrammering (get ent-javaprogrammering :id)
          new-id-kodprogrammering (get ent-kodprogrammering :id)
          analysis (sort comp-cpt (inf-ext/parse-text-experiment-with-text-compound-splitting "programmering"))
          correct (sort comp-cpt [{:id new-id-javaprogrammering, :preferredLabel "javaprogrammering", :type "skill"}
                                  {:id new-id-kodprogrammering, :preferredLabel "kodprogrammering", :type "skill"}])]
      (test/is (= analysis correct)))))
