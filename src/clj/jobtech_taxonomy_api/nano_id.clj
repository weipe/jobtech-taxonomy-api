(ns jobtech-taxonomy-api.nano-id
  (:gen-class)
  (:require [nano-id.custom :refer [generate]]
            [clojure.string :as str]))

(def base-58-nano-id (generate "123456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ"))

(defn generate-id []
  "Specify format and length of nano ID"
  (base-58-nano-id 10))

(defn add-underscore [id]
  (str (subs id 0 4) "_" (subs id 4 7)  "_" (subs id 7 10)))

(defn generate-new-id []
  (add-underscore (generate-id)))
