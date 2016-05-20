(ns salvador.handler-test
  (:require [clojure.test :refer :all]
            [salvador.handler :refer :all]))

(deftest swagger
  (is (not (nil? app)))
  (is (not (nil? (app {:request-method :get, :uri "/swagger.json"}))))
  (spit "target/swagger.json" (slurp (:body (app {:request-method :get, :uri "/swagger.json"})))))
