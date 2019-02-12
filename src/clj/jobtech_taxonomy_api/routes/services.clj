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
            [jobtech-taxonomy-api.db.core :refer :all]))

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

   (context "/taxonomy/public-api" []
     :tags ["public"]

     (GET "/term" []
       :return       String
       :query-params [term :- String]
       :summary      "get term"
       (ok (str (find-concept-by-preferred-term term))))

     (GET "/full-history" []
       :query-params []
       :summary      "Show the complete history."
       {:body (show-term-history)})

     (GET "/history-since" []
       :query-params [date-time :- String]
       :summary      "Show the history since the given date. Use the format '2017-06-09 14:30:01'."
       {:body (show-term-history-since (c/to-date (f/parse (f/formatter "yyyy-MM-dd hh:mm:ss") date-time)))}))

   (context "/taxonomy/private-api" []
     :tags ["private"]

     ;; GET /concept/all/<taxonomityp>
     ;; DELETE /concept/<id>  (obs retract)
     ;; POST /concept/is-deprecated -- skicka in IDn, returnera vilka av dessa som är deprecated:
     ;;                                { { id:<id>, referTo <new-id> }, ... }
     ;; POST /concept -- skapa nytt koncept. Skicka in:
     ;;                    preferredTerm
     ;;                    description
     ;;                    typ
     ;;                    alternativeTerms (optional - kolla om/hur det görs)

     ;; GET /concept/<id>
     (GET "/concept"    []
       :query-params [id :- String]
       :summary      "Get a concept by ID."
       {:body (find-concept-by-id id)})

     ;; GET /concept/types -- returnerar en lista över alla taxonomityper
     (GET "/concept/types"    []
       :query-params []
       :summary      "Get a list of all taxonomy types."
       {:body (get-all-taxonomy-types)})

     )))
