(ns project2-server.settings)

(def require-auth (boolean (not (System/getenv "WITHOUT_AUTH"))))

(def server-location (or (System/getenv "SERVER_LOCATION")
                         "http://localhost:3000"))
(def mongo-host (or (System/getenv "MONGO_HOST")
                    "localhost"))
(def mongo-port (Integer. (or (System/getenv "MONGO_PORT")
                              27017)))
(def mongo-dbname "syncsync")
(def mongo-images "images")
(def mongo-music "music")
(def mongo-contacts "contacts")
