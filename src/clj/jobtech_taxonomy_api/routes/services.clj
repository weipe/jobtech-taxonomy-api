(ns jobtech-taxonomy-api.routes.services
  (:refer-clojure :exclude [type])
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
   [jobtech-taxonomy-api.db.concepts :as concepts]
   [jobtech-taxonomy-api.db.search :as search]
   [jobtech-taxonomy-api.db.information-extraction :as ie]
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

   (context "/v0/taxonomy/public" []
     :tags ["public"]
     :auth-rules authenticated?

     (GET "/changes" []
       :query-params [fromDateTime :- String
                      {offset       :- Long 0}
                      {limit        :- Long 0}]
       :responses {200 {:schema show-changes-schema}
                   500 {:schema {:type s/Str, :message s/Str}}}
       :summary      "Show the history since the given date. Use the format yyyy-MM-dd HH:mm:ss (i.e. 2017-06-09 14:30:01)."
       (response/ok (show-changes-since (c/to-date (f/parse (f/formatter "yyyy-MM-dd HH:mm:ss") fromDateTime)) offset limit)))

     (GET "/concepts"    []
       :query-params [{id :- String nil}
                      {preferredLabel :- String nil}
                      {type :- String nil}
                      {deprecated :- Boolean false}
                      {offset :- Long nil}
                      {limit :- Long nil}
                      ]

       :responses {200 {:schema concepts/find-concepts-schema}
                   500 {:schema {:type s/Str, :message s/Str}}}
       :summary      "Get concepts."
       (response/ok (concepts/find-concepts id preferredLabel type deprecated offset limit)))

     (GET "/search" []
       :query-params [q       :- String
                      {type   :- String nil}
                      {offset :- Long nil}
                      {limit  :- Long nil}]
       :responses {200 {:schema search/get-concepts-by-search-schema}
                   500 {:schema {:type s/Str, :message s/Str}}}
       :summary      "Autocomplete from query string"
       (response/ok (search/get-concepts-by-search q type offset limit)))

     (GET "/deprecated-concept-history-since" []
       :query-params [date-time :- String]
       :responses {200 {:schema s/Any} ;; show-concept-events-schema} TODO FIXME
                   500 {:schema {:type s/Str, :message s/Str}}}
       :summary      "Show the history since the given date. Use the format yyyy-MM-dd HH:mm:ss (i.e. 2017-06-09 14:30:01)."
       (response/ok (show-deprecated-concepts-and-replaced-by (c/to-date (f/parse (f/formatter "yyyy-MM-dd HH:mm:ss") date-time)))))

     (GET "/concept/types"    []
       :query-params []
       :responses {200 {:schema [ s/Str ]}
                   500 {:schema {:type s/Str, :message s/Str}}}
       :summary "Return a list of all taxonomy types."
       {:body (get-all-taxonomy-types)})


     (POST "/parse-text"    []
       :query-params [text :- String]
       :responses {200 {:schema [ s/Any]}
                   500 {:schema {:type s/Str, :message s/Str}}}
       :summary "Finds all concepts in a text."
       {:body (ie/parse-text text)})
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
                      definition :- String
                      preferredLabel :- String]
       :summary      "Assert a new concept."
       {:body (assert-concept type definition preferredLabel)})

     (POST "/replace-concept"    []
       :query-params [old-concept-id :- String
                      new-concept-id :- String]
       :summary      "Replace old concept with a new concept."
       {:body (replace-deprecated-concept old-concept-id new-concept-id)})

          (GET "/relation/graph/:relation-type" []
       :path-params [relation-type :- String]
       :responses {200 {:schema s/Any}
                   500 {:schema {:type s/Str, :message s/Str}}}
       :summary "Relation graphs."
       (response/ok (get-relation-graph (keyword relation-type))))

     (GET "/relation/graph/:relation-type/:id" []
       :path-params [relation-type :- String
                     id :- String]
       :responses {200 {:schema s/Any}
                   500 {:schema {:type s/Str, :message s/Str}}}
       :summary "Relation graphs."
       (response/ok (get-relation-graph-from-concept (keyword relation-type) id)))

     (GET "/relation/types" []
       :responses {200 {:schema s/Any}
                   500 {:schema {:type s/Str, :message s/Str}}}
       :summary "Relation graphs."
       (response/ok (get-relation-types))))))
