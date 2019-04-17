(ns jobtech-taxonomy-api.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [jobtech-taxonomy-api.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[jobtech-taxonomy-api started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[jobtech-taxonomy-api has shut down successfully]=-"))
   :middleware wrap-dev})
