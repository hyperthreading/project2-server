(ns project2-server.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defroutes app-routes
  (GET "/" [] "Hello World!!!!")
  (GET "/something-inside" [] "LOL")
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
