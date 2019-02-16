(ns jobtech-taxonomy-api.nano-id
  (:gen-class)
  (:require [nano-id.custom :refer [generate]]
            [clojure.string :as str]
            ))

(def base-59-nano-id (generate "123456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ_"))

(defn generate-new-id
  "Specify format and length of nano ID"
  []
  (base-59-nano-id 10))
