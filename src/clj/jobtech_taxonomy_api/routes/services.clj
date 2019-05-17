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
   [buddy.auth.http :as http]
   [clojure.data.json :as json]
   [clj-time [format :as f]]
   [clj-time.coerce :as c]
   [jobtech-taxonomy-api.db.core :refer :all]
   [jobtech-taxonomy-api.middleware :as middleware]
   [clojure.tools.logging :as log]
   [clojure.pprint :as pp]))

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

(defn authorized-private? [request]
  (= (http/-get-header request "api-key") (middleware/get-token :admin)))

(def service-routes
  (api
   {:exceptions
    {:handlers
     {::ex/default (custom-handler response/internal-server-error :fatal)}}
    :swagger {:ui "/v0/taxonomy/swagger-ui"
              :spec "/v0/taxonomy/swagger.json"
              :data {:info {:version "0.9.0"
                            :title "Jobtech Taxonomy"
                            :description "Jobtech taxonomy services"}
                     ;; API header config found here: https://gist.github.com/Deraen/ef7f65d7ec26f048e2bb
                     :securityDefinitions {:api_key {:type "apiKey" :name "api-key" :in "header"}}}}}

   #_(GET "/authenticated" []
     :current-user user
     (response/ok {:user user}))


   (context "/v0/taxonomy/public" []
     :tags ["public"]
     :auth-rules authenticated?



 (GET "/relation/graph/:relation-type" []
     :path-params [relation-type :- String]
     :responses {200 {:schema s/Any}
                 404 {:schema {:reason (s/enum :NOT_FOUND)}}
                 500 {:schema {:type s/Str, :message s/Str}}}
     :summary "Relation graphs."
     (let [result (get-relation-graph (keyword relation-type))] ; (keyword taxonomy)
       (if (not-empty result)
         (response/ok result)
         (response/not-found {:reason :NOT_FOUND}))))

   (GET "/relation/graph/:relation-type/:id" []
     :path-params [relation-type :- String
                   id :- String]
     :responses {200 {:schema s/Any}
                 404 {:schema {:reason (s/enum :NOT_FOUND)}}
                 500 {:schema {:type s/Str, :message s/Str}}}
     :summary "Relation graphs."
     (let [result (get-relation-graph-from-concept (keyword relation-type) id)]
       (if (not-empty result)
         (response/ok result)
         (response/not-found {:reason :NOT_FOUND}))))

   (GET "/relation/types" []
     :responses {200 {:schema s/Any}
                 500 {:schema {:type s/Str, :message s/Str}}}
     :summary "Relation graphs."
     (let [result (get-relation-types)]
       (if (not-empty result)
         (response/ok result)
         (response/not-found {:reason :NOT_FOUND}))))






     (GET "/term" []
       :query-params [term :- String]
       :responses {200 {:schema get-concepts-by-term-start-schema}
                   404 {:schema {:reason (s/enum :NOT_FOUND)}}
                   500 {:schema {:type s/Str, :message s/Str}}}
       :summary "Search for a term across all taxonomies."
       (let [result (find-concept-by-preferred-term term)]
         (if (not-empty result)
           (response/ok result)
           (response/not-found {:reason :NOT_FOUND}))))

     (GET "/term-part" []
       :query-params [term :- String]
       :responses {200 {:schema get-concepts-by-term-start-schema}
                   404 {:schema {:reason (s/enum :NOT_FOUND)}}
                   500 {:schema {:type s/Str, :message s/Str}}}
       :summary      "get concepts by part of string"
       (let [result (take 10 (get-concepts-by-term-start term))]
         (if (not-empty result)
           (response/ok result)
           (response/not-found {:reason :NOT_FOUND}))))

;; Jag tog bort den eftersom den tar 15 sekunder att köra. Vi får hitta något annat sätt att dumpa databasen på.

     (GET "/full-history" []
       :query-params []
       :responses {200 {:schema show-concept-events-schema}
                   500 {:schema {:type s/Str, :message s/Str}}}
       :summary      "Show the complete history."
       (response/ok (show-concept-events)))



     (GET "/changes" []
       :query-params [fromDateTime :- String
                      {offset       :- Integer 0}
                      {limit        :- Integer 0}]
       :responses {200 {:schema show-changes-schema}
                   404 {:schema {:reason (s/enum :NOT_FOUND)}}
                   500 {:schema {:type s/Str, :message s/Str}}}
       :summary      "Show the history since the given date. Use the format yyyy-MM-dd HH:mm:ss (i.e. 2017-06-09 14:30:01)."
       (let [result (show-changes-since (c/to-date (f/parse (f/formatter "yyyy-MM-dd HH:mm:ss") fromDateTime)) offset limit)]
         (if (not (empty? result))
           (response/ok result)
           (response/not-found {:reason :NOT_FOUND}))))


     (GET "/concept-history-since" []
       :query-params [date-time :- String]
       :responses {200 {:schema show-concept-events-schema}
                   404 {:schema {:reason (s/enum :NOT_FOUND)}}
                   500 {:schema {:type s/Str, :message s/Str}}}
       :summary      "Show the history since the given date. Use the format yyyy-MM-dd HH:mm:ss (i.e. 2017-06-09 14:30:01)."
       (let [result (show-concept-events-since (c/to-date (f/parse (f/formatter "yyyy-MM-dd HH:mm:ss") date-time)))]
         (if (not (empty? result))
           (response/ok result)
           (response/not-found {:reason :NOT_FOUND}))))

     (GET "/deprecated-concept-history-since" []
       :query-params [date-time :- String]
       :responses {200 {:schema s/Any} ;; show-concept-events-schema} TODO FIXME
                   404 {:schema {:reason (s/enum :NOT_FOUND)}}
                   500 {:schema {:type s/Str, :message s/Str}}}
       :summary      "Show the history since the given date. Use the format yyyy-MM-dd HH:mm:ss (i.e. 2017-06-09 14:30:01)."
       (let [result (show-deprecated-concepts-and-replaced-by (c/to-date (f/parse (f/formatter "yyyy-MM-dd HH:mm:ss") date-time)))]
         (if (not (empty? result))
           (response/ok result)
           (response/not-found {:reason :NOT_FOUND}))))


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

     )



   (context "/v0/taxonomy/private" []
     :tags ["private"]
            ;;:auth-rules {:or [swagger-ui-user? (fn [req] (and (authenticated? req) (authorized-private? req)))]}
     :auth-rules {:and [authenticated? authorized-private?]}


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
