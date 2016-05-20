;; lein new compojure-api k3
;; cd k3
;; replace handler.clj with this
;; lein ring server
(ns salvador.handler
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [clojure.java.io :as io]
            [org.httpkit.client :as http]
            [ring-aws-lambda-adapter.core :refer [defhandler]]
            [salvador.core :as env]
            [clostache.parser :refer [render]]
            [clojure.walk :refer [keywordize-keys]]
            [plumbing.core :refer [fnk]]))

(def aws-gateway-options
  {:x-amazon-apigateway-integration
   {:responses {:default {:statusCode "200"
                          :responseTemplates {"application/json" "$input.json('$.body')"
                                              "text/html" "$input.json('$body.template-body')"}}}
    :requestTemplates { "application/json" (slurp (io/resource "bodymapping.vm")) }
    :uri (str "arn:aws:apigateway:us-east-1:lambda:path/2015-03-31/functions/" (env/env :lambda-arn) "/invocations")
    :httpMethod "POST"
    :type "aws"}})

(def template-resource
  (resource
    {:get
     (merge
       aws-gateway-options
       {:responses {200 {:schema s/Any :description "Default response"}
                    302 {:schema s/Any :description "Redirect to `continue` location"}
                    500 {:schema s/Any :description "Horror"}}
        :parameters {:query-params {:template String}}
        :summary "Render template with request map"
        :handler (fn [request]
                   (let [template (-> request :query-params :template)
                         parameters (dissoc (keywordize-keys (:query-params request)) :template)
                         {:keys [status headers body error] :as string} @(http/get template)]
                     {:status 200
                      :headers {"content-type" "text/html" ;gets overwritten
                                "X-Content-Type" "text/html"}
                      :body {:template-source template
                          :parameters parameters
                          :template-body body
                          :content-type "text/html"
                          :template-expanded (render body parameters)}}))})}))

(def app
  (api
    {:swagger
     {:ui "/"
      :spec "/swagger.json"}}
      (context "/render" [] template-resource)))

(defhandler salvador.handler.Lambda app {})
