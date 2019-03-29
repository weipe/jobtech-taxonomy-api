(ns jobtech-taxonomy-api.apikey
  "The token based authentication and authorization backend."
  (:require [buddy.auth.protocols :as proto]
            [buddy.auth.http :as http]
            [buddy.auth :refer [authenticated?]]
            [buddy.sign.jwt :as jwt]))

(defn- handle-unauthorized-default
  "A default response constructor for an unauthorized request."
  [request]
  (if (authenticated? request)
    {:status 403 :headers {} :body "Permission denied"}
    {:status 401 :headers {} :body "Unauthorized"}))

(defn- parse-header
  [request token-name]
  (http/-get-header request "api-key"))

(defn apikey-backend
  [{:keys [authfn unauthorized-handler token-name] :or {token-name "api-key"}}]
  ;;{:pre [(ifn? authfn)]}
  (reify
    proto/IAuthentication
    (-parse [_ request]
      (parse-header request token-name))
    (-authenticate [_ request token]
      (authfn request token))

    proto/IAuthorization
    (-handle-unauthorized [_ request metadata]
      (if unauthorized-handler
        (unauthorized-handler request metadata)
        (handle-unauthorized-default request)))))
