(ns project2-server.settings)

(def server-location (System/getenv "SERVER_LOCATION"))
(def mongo-host (System/getenv "MONGO_HOST"))
(def mongo-port (Integer. (System/getenv "MONGO_PORT")))
(def mongo-dbname "syncsync")
(def mongo-images "images")
(def mongo-contacts "contacts")
