(ns project2-server.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults site-defaults]]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [monger.core :as mg]
            [monger.collection :as mc]
            [project2-server.cognito-auth :refer [wrap-cognito-authorization
                                                  wrap-cognito-authentication]]))

(def server-location "http://143.248.36.226:3000")
(def mongo-host "localhost")
(def mongo-port 32769)
(def mongo-dbname "syncsync")
(def mongo-images "images")
(def mongo-contacts "contacts")
(defonce mongo-conn (mg/connect {:host mongo-host :port mongo-port}))
(defonce mongo-db (mg/get-db mongo-conn mongo-dbname))


"For the record, due to multipart middleware, Attached files of a form data are saved as temp file"
"TODO:: checking failed operation"

(defn image-get-from-database
  []
  {:content
   (map #(dissoc % :_id)
        (mc/find-maps mongo-db mongo-images {}))})

(defn image-get-response
  [q]
  (image-get-from-database))

(defn image-add-to-database [data]
  (mc/insert mongo-db
             mongo-images
             data))

(defn image-add-response
  "Saves image to resources/static and returns a result"
  [params metadata]
  (let [fileArgs        (get metadata "fileName")
        uuid            (java.util.UUID/randomUUID)
        target-filename (str uuid ".jpg")
        url             (str server-location "/" target-filename)]
    (let [data   (get-in params [fileArgs :tempfile])
          target (io/file (str "./resources/static/" target-filename))]
      (io/copy data target))
    (image-add-to-database {:uuid      (str uuid)
                            :metadata  {:uploadedAt "2017-10-12"
                                        :createdAt  "2017-08-21"
                                        :name       (get metadata "name")}
                            :thumbnail url
                            :url       url
                            :user      "hpthrd"})
    {:fileName fileArgs
     :msg      "success"
     :url      url
     :uuid     (str uuid)}))

(defn image-remove-from-database
  [uuid]
  (mc/remove mongo-db mongo-images
             {:uuid uuid}))

(defn image-remove-response
  [uuid]
  (do (image-remove-from-database uuid)
      {:uuid uuid
       :msg "success"}))

(defn contact-get-from-database
  [q]
  (mc/find-maps mongo-db mongo-contacts {}))

(defn contact-get-response
  []
  {:content
   (map #(dissoc % :_id)
        (contact-get-from-database ""))})

(defn contact-add-to-database
  [data]
  (mc/insert mongo-db mongo-contacts
             data))

(defn contact-add-response
  "Saves a given contact to our collection `contacts` and returns result"
  [{:keys [name phone email]}]
  (let [uuid (java.util.UUID/randomUUID)
        new-data {:uuid (str uuid)
                  :name name
                  :phone phone
                  :email email
                  :user "hpthrd"}]
    (contact-add-to-database new-data)
    (assoc new-data :msg "success")))

(defn contact-remove-from-database
  [uuid]
  (mc/remove mongo-db mongo-contacts {:uuid uuid}))

(defn contact-remove-response
  "Removes a contact"
  [uuid]
  (do (contact-remove-from-database uuid)
      {:msg "success"
       :uuid uuid}))

"TODO:: Add post functionality"
(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/contacts" [] (json/write-str (contact-get-response)))
  (GET "/contacts/:uuid" [uuid] uuid)
  (DELETE "/contacts/:uuid" [uuid] (json/write-str (contact-remove-response uuid)))
  (POST "/contacts" {body :body}
        (json/write-str {:msg "success"
                         :result (let [json-str (with-open [reader (io/reader body)]
                                                  (json/read reader
                                                   :key-fn keyword))]
                                   (prn json-str)
                                   (map contact-add-response
                                        (get json-str :content)))}))
  (GET "/photos" [] (json/write-str (image-get-response "")))
  (GET "/photos" [q] (json/write-str (image-get-response q)))
  (POST "/photos" [metadata :as {:keys [multipart-params] :as req}]
    (do (prn req)
        (json/write-str {:msg "success"
                         :result (map (partial image-add-response multipart-params)
                                      (get (json/read-str metadata) "metadata"))})))
  (DELETE "/photos/:uuid" [uuid] (json/write-str (image-remove-response uuid)))
  (route/not-found "Not Found"))

(def app
  (-> (wrap-defaults app-routes (assoc-in (assoc-in api-defaults [:params :multipart] {})
                                          [:static :resources] "static"))
      (wrap-cognito-authorization)
      (wrap-cognito-authentication)))

