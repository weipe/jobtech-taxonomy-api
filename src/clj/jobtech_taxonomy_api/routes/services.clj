(ns jobtech-taxonomy-api.routes.services
  (:require [ring.util.http-response :refer :all]
            [ring.middleware.json :refer [wrap-json-response]]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [compojure.api.meta :refer [restructure-param]]
            [buddy.auth.accessrules :refer [restrict]]
            [buddy.auth :refer [authenticated?]]
            [clojure.data.json :as json]
            [clj-time [format :as f]]
            [clj-time.coerce :as c]
            [jobtech-taxonomy-api.db.core :refer [find-concept-by-preferred-term show-term-history show-term-history-since]]
            ))

(defn access-error [_ _]
  (unauthorized {:error "unauthorized"}))

(defn wrap-restricted [handler rule]
  (restrict handler {:handler  rule
                     :on-error access-error}))

(defmethod restructure-param :auth-rules
  [_ rule acc]
  (update-in acc [:middleware] conj [wrap-restricted rule]))

(defmethod restructure-param :current-user
  [_ binding acc]
  (update-in acc [:letks] into [binding `(:identity ~'+compojure-api-request+)]))

(def service-routes
  (api
    {:swagger {:ui "/taxonomy/swagger-ui"
               :spec "/taxonomy/swagger.json"
               :data {:info {:version "1.0.0"
                             :title "Sample API"
                             :description "Sample Services"}}}}

    (GET "/authenticated" []
         :auth-rules authenticated?
         :current-user user
         (ok {:user user}))

    (context "/taxonomy/api" []
      :tags ["thingie"]

      (GET "/term" []
        :return       String
        :query-params [term :- String]
        :summary      "get term"
        (ok (str (find-concept-by-preferred-term term))))

      (GET "/full-history" []
           :query-params []
           :summary      "Show the complete history."
           {:body (show-term-history)})

      ;; TODO: debug, seems to be date casting problems or something, try
      ;;    (show-term-history-since (c/to-date (f/parse (f/formatter "yyyy-MM-dd") "2017-10-10")))
      (GET "/history-since" []
           :query-params [date-time :- String]
           :summary      "Show the history since the given date. Use the format '2017-06-09'."
           {:body (show-term-history-since (c/to-date (f/parse (f/formatter "yyyy-MM-dd") date-time)))})

      )))
