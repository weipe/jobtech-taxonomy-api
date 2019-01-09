(ns user
  (:require [jobtech-taxonomy-api.config :refer [env]]
            [clojure.spec.alpha :as s]
            [expound.alpha :as expound]
            [mount.core :as mount]
            [jobtech-taxonomy-api.core :refer [start-app]]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(defn start []
  (mount/start-without #'jobtech-taxonomy-api.core/repl-server))

(defn stop []
  (mount/stop-except #'jobtech-taxonomy-api.core/repl-server))

(defn restart []
  (stop)
  (start))


