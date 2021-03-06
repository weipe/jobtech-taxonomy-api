(ns jobtech-taxonomy-api.routes.services
  (:require
   [ring.util.http-response :as response]
   [ring.middleware.json :refer [wrap-json-response]]
   [compojure.api.sweet :refer :all]
   [schema.core :as s]
   [compojure.api.meta :refer [restructure-param]]
   [compojure.api.exception :as ex]
   [buddy.auth.accessrules :refer [restrict]]
   [buddy.auth :refer [authenticated?]]
   [clojure.data.json :as json]
   [clj-time [format :as f]]
   [clj-time.coerce :as c]
   [jobtech-taxonomy-api.db.core :refer :all]
   [clojure.tools.logging :as log]))

(import java.util.Date)

(def date? (partial instance? Date))

(def date-validator
  {:message "must be a Date"
   :optional true
   :validate date?})

(defn access-error [_ _]
  (response/unauthorized {:error "unauthorized"}))

(defn wrap-restricted [handler rule]
  (restrict handler {:handler  rule
                     :on-error access-error}))

(defmethod restructure-param :auth-rules
  [_ rule acc]
  (update-in acc [:middleware] conj [wrap-restricted rule]))

(defmethod restructure-param :current-user
  [_ binding acc]
  (update-in acc [:letks] into [binding `(:identity ~'+compojure-api-request+)]))

(defn custom-handler [f type]
  (fn [^Exception e data request]
    (log/log type (.getMessage e))
    (f {:message (.getMessage e), :type type})))

(def service-routes
  (api
   {:exceptions
    {:handlers
     {java.lang.AssertionError (custom-handler response/not-found :error)
      java.lang.IllegalArgumentException (ex/with-logging ex/request-parsing-handler :error)
      ::ex/default (custom-handler response/internal-server-error :unknown)}}

    :swagger {:ui "/taxonomy/swagger-ui"
              :spec "/taxonomy/swagger.json"
              :data {:info {:version "1.0.0"
                            :title "Jobtech Taxonomy"
                            :description "Jobtech taxonomy services"}}}}

   (GET "/authenticated" []
     :auth-rules authenticated?
     :current-user user
     (response/ok {:user user}))

   (context "/taxonomy/public-api" []
     :tags ["public"]

     (GET "/term" []
       :query-params [term :- String]
       :summary      "get term"
       :return       find-concept-by-preferred-term-schema
       {:body (find-concept-by-preferred-term term)})

     (GET "/full-history" []
       :query-params []
       :summary      "Show the complete history."
       :return       show-concept-events-schema
       {:body (show-concept-events)})

     (GET "/concept-history-since" []
       :query-params [date-time :- String]
       :summary      "Show the history since the given date. Use the format '2017-06-09 14:30:01'."
       :return       show-concept-events-schema
       {:body (show-concept-events-since (c/to-date (f/parse (f/formatter "yyyy-MM-dd HH:mm:ss") date-time)))})

     (GET "/deprecated-concept-history-since" []
       :query-params [date-time :- String]
       :summary      "Show the history since the given date. Use the format '2017-06-09 14:30:01'."
       {:body (show-deprecated-concepts-and-replaced-by (c/to-date (f/parse (f/formatter "yyyy-MM-dd HH:mm:ss") date-time)))}))

   (context "/taxonomy/private-api" []
     :tags ["private"]

     ;; POST /concept/is-deprecated -- skicka in IDn, returnera vilka av dessa som är deprecated:
     ;;                                { { id:<id>, referTo <new-id> }, ... }

     (GET "/concept"    []
       :query-params [id :- String]
       :summary      "Read a concept by ID."
       {:body (find-concept-by-id id)})

     (GET "/concept/types"    []
       :query-params []
       :summary      "Read a list of all taxonomy types."
       {:body (get-all-taxonomy-types)})

     (GET "/concept/all"    []
       :query-params [type :- String]
       :summary      "Read all concepts of the given type."
       {:body (get-concepts-for-type type)})

     (DELETE "/concept"    []
       :query-params [id :- String]
       :summary      "Retract the concept with the given ID."
       {:body (retract-concept id)})

     ;; alternativeTerms (optional - kolla om/hur det görs)
     (POST "/concept"    []
       :query-params [type :- String
                      description :- String
                      preferredTerm :- String]
       :summary      "Assert a new concept."
       {:body (assert-concept type description preferredTerm)})

     (POST "/replace-concept"    []
       :query-params [old-concept-id :- String
                      new-concept-id :- String]
       :summary      "Replace old concept with a new concept."
       {:body (replace-deprecated-concept old-concept-id new-concept-id)}))))
