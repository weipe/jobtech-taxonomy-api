(ns jobtech-taxonomy-api.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[jobtech-taxonomy-api started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[jobtech-taxonomy-api has shut down successfully]=-"))
   :middleware identity})
