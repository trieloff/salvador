;; lein new compojure-api k3
;; cd k3
;; replace handler.clj with this
;; lein ring server
(ns salvador.handler
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [clojure.java.io :as io]
            [ring-aws-lambda-adapter.core :refer [defhandler]]
            [salvador.core :as env]
            [plumbing.core :refer [fnk]]))

(def aws-gateway-options
  {:x-amazon-apigateway-integration
   {:responses {:default {:statusCode "200"
                          :responseTemplates {"application/json" "$input.json('$.body')"}}}
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
                    500 {:schema {:code String} :description "Horror"}}
        :parameters {:query-params {:template String}}
        :summary "Render template with request map"
        :handler (fnk [[:query-params template]]
                    (ok [template]))})}))

(def app
  (api
    {:swagger
     {:ui "/"
      :spec "/swagger.json"}}
      (context "/render" [] template-resource)))

(defhandler salvador.handler.Lambda app {})
