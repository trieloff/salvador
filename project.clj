(defproject salvador "0.1.0-SNAPSHOT"
  :description "Mustache via HTTP"
  :url "https://github.com/trieloff/salvador"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [metosin/compojure-api "1.1.1"]
                 [environ "1.0.2"]
                 [http-kit "2.1.19"]
                 [ring-aws-lambda-adapter "0.1.1"]
                 [de.ubercode.clostache/clostache "1.4.0"]]
  :ring {:handler salvador.handler/app}
  :uberjar-name "server.jar"
  :resource-paths ["resources"
                   ".lein-env"] ; let's see if we can sneak the environment variables into the binary
  :plugins [[test2junit "1.1.2"]
            [lein-environ "1.0.2"]
            [lein-aws-api-gateway "1.10.68-1"]
            [lein-clj-lambda "0.4.0"]
            [lein-maven-s3-wagon "0.2.5"]]
  :api-gateway {:api-id "c8sc0xfjf7"
                :swagger "target/swagger.json"}

  :test2junit-output-dir ~(or (System/getenv "CIRCLE_TEST_REPORTS") "target/test2junit")
  :env {:aws-access-key #=(eval (System/getenv "AWS_ACCESS_KEY"))
        :lambda-arn #=(eval (System/getenv "LAMBDA_ARN"))
        :aws-secret-key #=(eval (System/getenv "AWS_SECRET_KEY"))}
  :lambda {"dev" [{:handler "salvador.handler.Lambda"
                  :memory-size 512
                  :timeout 300
                  :function-name "salvador-dev"
                  :region "us-east-1"
                  :s3 {:bucket "leinrepo"
                       :object-key "salvador-dev.jar"}}]
         "production" [{:handler "salvador.handler.Lambda"
                        :memory-size 512
                        :timeout 300
                        :function-name "salvador-prod"
                        :region "us-east-1"
                        :s3 {:bucket "leinrepo"
                            :object-key "salvador-release.jar"}}]}
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]]
                   :plugins [[lein-ring "0.9.7"]
                             [lein-dynamodb-local "0.2.8"]]}
             :uberjar {:main salvador.core :aot :all}})
