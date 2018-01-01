(ns project2-server.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults site-defaults]]
            [clojure.data.json :as json]
            [clojure.java.io :as io]))

(def contacts (atom {:content [{:uuid "62183b10-7b96-4929-a2ea-0536ee776b0f"
                                :name "류석영"
                                :email "ryu.sukyoung@gmail.com"
                                :phone "010-1234-5678"}
                               {:uuid "a457656a-9632-4a9c-b0b2-f0515c668590"
                                :name "장병규"
                                :email "chitos@bluehole.com"
                                :phone "010-1234-5678"}]}))

(def onecat "https://news.nationalgeographic.com/content/dam/news/photos/000/755/75552.adapt.590.1.jpg")
(def twocat "https://static.boredpanda.com/blog/wp-content/uploads/2016/09/cats-toothless-lookalikes-3-57ce7b4a6f3e9__700.jpg")
(def threecat "http://i.imgur.com/O9n2213.jpg")
(def fourcat "https://cms.hostelbookers.com/hbblog/wp-content/uploads/sites/3/2012/02/cat-happy-cat-e1329931204797.jpg")

(def photos (atom {:content [{:uuid "7ab7a3d1-e171-49cd-b014-873549e266a1"
                              :metadata {:createdAt "2017-08-21"}
                              :thumbnail onecat
                              :url onecat}
                             {:uuid "df2c0eee-a3fd-4087-ba83-53cd1014e24f"
                              :metadata {:createdAt "2017-08-21"}
                              :thumbnail twocat
                              :url twocat}
                             {:uuid "db25f7be-ebcf-4b17-89e4-411d15a699a9"
                              :metadata {:createdAt "2017-08-21"}
                              :thumbnail threecat
                              :url threecat}
                             {:uuid "22b51240-6fa9-45c0-84da-054fd6da6293"
                              :metadata {:createdAt "2017-08-21"}
                              :thumbnail fourcat
                              :url fourcat}
                             ]}))

  (def server-location "http://143.248.36.226:3000")

; (get (json/read-str (json/write-str contacts)) "content")

"For the record, due to multipart middleware, Attached files of a form data are saved as temp file"
(defn save-image
  "Saves image to resources/static and returns result"
  [params metadata]
  (let [fileArgs (get metadata "fileName")
        uuid (java.util.UUID/randomUUID)
        target-filename (str uuid ".jpg")]
    (do (prn metadata)
        (let [data (get-in params [(keyword fileArgs) :tempfile])
              target (io/file (str "./resources/static/" target-filename))]
          (do (prn data)
              (io/copy data target)))
        (json/write-str {:fileName fileArgs
                         :msg "success"
                         :url (str server-location "/" target-filename)
                         :uuid (str uuid)}))))

"TODO: Add post functionality"
(defroutes app-routes
  (GET "/" [] "Hello Clojure!!!!")
  (GET "/contacts" [] (json/write-str @contacts))
  (POST "/contacts" [metadata :as req] (do (prn req) metadata))
  (GET "/photos" [] (json/write-str @photos))
  (POST "/photos" [metadata :as {:keys [params] :as req}]
        (do (prn (str "received " metadata))
            (json/write-str {:msg "success"
                             :result (map (partial save-image params)
                                          (get (json/read-str metadata) "metadata"))})))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes (assoc-in (assoc-in api-defaults [:params :multipart] {})
                                      [:static :resources] "static")))

