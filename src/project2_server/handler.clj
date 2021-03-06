(ns project2-server.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults site-defaults]]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [monger.core :as mg]
            [monger.collection :as mc]
            [project2-server.cognito-auth :refer [wrap-cognito-authorization
                                                  wrap-cognito-authentication
                                                  wrap-cognito-mock-authn] :as cognito]
            [project2-server.settings :refer :all :as settings]
            [project2-server.util :refer :all]
            [cemerick.url :as url]))

(defonce mongo-conn (mg/connect {:host mongo-host :port mongo-port}))
(defonce mongo-db (mg/get-db mongo-conn mongo-dbname))


"For the record, due to multipart middleware, Attached files of a form data are saved as temp file"
"TODO:: checking failed operation"

(defn image-get-from-database
  [query]
  {:content
   (map #(dissoc % :_id)
        (mc/find-maps mongo-db mongo-images query))})

(defn image-get-response
  [user-id additional-query]
  (image-get-from-database (merge additional-query {:user user-id})))

(defn image-add-to-database [data]
  (mc/insert mongo-db
             mongo-images
             data))

(defn image-add-response
  "Saves image to resources/static and returns a result"
  [user-id params metadata]
  (let [fileArgs        (get metadata "fileName")
        uuid            (java.util.UUID/randomUUID)
        target-filename (str uuid ".jpg")
        url             (str server-location "/" target-filename)]
    (let [data   (get-in params [fileArgs :tempfile])
          target (io/file (str "./resources/static/" target-filename))]
      (io/copy data target))
    (image-add-to-database {:uuid      (str uuid)
                            :metadata  {:uploadedAt  "2017-10-12"
                                        :createdAt   "2017-08-21"
                                        :name        (get metadata "name")
                                        :orientation (get metadata "orientation")}
                            :thumbnail url
                            :url       url
                            :user      user-id})
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
  [query]
  (mc/find-maps mongo-db mongo-contacts query))

(defn contact-get-response
  [user-id]
  {:content
   (map #(dissoc % :_id)
        (contact-get-from-database {:user user-id}))})

(defn contact-add-to-database
  [data]
  (mc/insert mongo-db mongo-contacts
             data))

(defn contact-add-response
  "Saves a given contact to our collection `contacts` and returns result"
  [user-id {:keys [name phone email]}]
  (let [uuid (java.util.UUID/randomUUID)
        new-data {:uuid (str uuid)
                  :name name
                  :phone phone
                  :email email
                  :user user-id}]
    (contact-add-to-database new-data)
    (assoc new-data :msg "success")))

(defn contact-update-database
  [uuid doc]
  (mc/update mongo-db
             settings/mongo-contacts
             {:uuid uuid}
             {"$set" doc}))

(def contact-update-fields [:name :phone :email])

(defn contact-update-response
  [uuid json]
  (do (contact-update-database uuid
                               (get-map-with-not-nil json
                                                     contact-update-fields))
      {:msg "success"
       :uuid uuid}))

(defn contact-remove-from-database
  [uuid]
  (mc/remove mongo-db mongo-contacts {:uuid uuid}))

(defn contact-remove-response
  "Removes a contact"
  [uuid]
  (do (contact-remove-from-database uuid)
      {:msg "success"
       :uuid uuid}))

(defn music-get-from-database
  [query]
  {:content
   (map #(dissoc % :_id)
        (mc/find-maps mongo-db
                      settings/mongo-music
                      query))})

(defn music-get-response
  [user-id additional-query]
  (music-get-from-database (merge additional-query {:user user-id})))

(defn music-add-to-database [data]
  (mc/insert mongo-db
             settings/mongo-music
             data))

(defn music-add-response
  "Saves music to resources/static and returns a result"
  [user-id {params :multipart-params} metadata]
  (let [file-field      (get metadata "fileName")
        uuid            (java.util.UUID/randomUUID)
        target-filename (str uuid "." (extract-file-ext file-field "mp3"))
        url             (str server-location "/" target-filename)
        data            (get-in params [file-field :tempfile])
        target          (io/file (str "./resources/static/" target-filename))
        thumb-field     (get metadata "thumbnail")
        thumb-uuid      (and thumb-field (java.util.UUID/randomUUID))
        thumb-filename  (and thumb-field (str thumb-uuid "." (extract-file-ext thumb-field "jpg")))
        thumb-url       (and thumb-field (str server-location "/" thumb-filename))]
    
    (io/copy data target)
    (if thumb-field
      (let [data-thumb   (get-in params [thumb-field :tempfile])
            target-thumb (io/file (str "./resources/static/" thumb-filename))]
        (io/copy data-thumb target-thumb)))
    (music-add-to-database {:uuid          (str uuid)
                            :metadata      {:uploadedAt "2017-10-12"
                                            :createdAt  "2017-08-21"
                                            :title      (get metadata "title")
                                            :artist     (get metadata "artist")}
                            :thumbnail_url thumb-url
                            :url           url
                            :user          user-id})
    {:fileName      file-field
     :msg           "success"
     :url           url
     :thumbnail_url thumb-url
     :uuid          (str uuid)}))

(defn music-remove-from-database
  [uuid]
  (mc/remove mongo-db
             settings/mongo-music
             {:uuid uuid}))

(defn music-remove-response
  [uuid]
  (do (music-remove-from-database uuid)
      {:uuid uuid
       :msg "success"}))

"TODO:: Add post functionality"
(defroutes app-routes
  (GET "/" [] "Hello World")

  (GET "/contacts"
      {identity :identity}
    (json/write-str (contact-get-response (cognito/get-user-id identity))))
  (GET "/contacts/:uuid"
      [uuid]
    uuid)
  (DELETE "/contacts/:uuid"
      [uuid]
    (json/write-str (contact-remove-response uuid)))
  (PUT "/contacts/:uuid"
      [uuid :as {body :body}]
    (json/write-str (contact-update-response uuid
                                             (read-json-from-stream body
                                                                    :key-fn keyword))))
  (POST "/contacts"
      {body :body identity :identity}
    (json/write-str {:msg    "success"
                     :result (let [json-str (read-json-from-stream body
                                                                   :key-fn keyword)
                                   user-id (cognito/get-user-id identity)]
                               (prn json-str)
                               (map (partial contact-add-response user-id)
                                    (get json-str :content)))}))

  (GET "/photos"
      [q :as {identity :identity}]
    (json/write-str (image-get-response (cognito/get-user-id identity)
                                        (and q (not= q "")
                                             (json/read-str (url/url-decode q))))))
  (POST "/photos"
      [metadata :as {:keys [multipart-params identity] :as req}]
    (json/write-str {:msg    "success"
                     :result (map (partial image-add-response
                                           (cognito/get-user-id identity)
                                           multipart-params)
                                  (get (json/read-str metadata) "metadata"))}))
  (DELETE "/photos/:uuid"
      [uuid]
    (json/write-str (image-remove-response uuid)))

  (GET "/music"
      [q :as {identity :identity}]
    (json/write-str (music-get-response (cognito/get-user-id identity)
                                        (and q (not= q "")
                                             (json/read-str (url/url-decode q))))))
  (POST "/music"
      [metadata :as {identity :identity :as req}]
    (json/write-str {:msg    "success"
                     :result (map (partial music-add-response
                                           (cognito/get-user-id identity)
                                           req)
                                  (get (json/read-str metadata) "metadata"))}))
  (DELETE "/music/:uuid"
      [uuid]
    (json/write-str (music-remove-response uuid)))


  (route/not-found "Not Found"))


(defn wrap-logger
  [handler]
  (fn [request]
    (do (println (:request-method request)
                 (:uri request))
        (let [response (handler request)]
          (println (:status response)
               (:body response))
          response))))

(defn wrap-auth-setting
  [ruote wrapper alter]
  (if settings/require-auth
    (wrapper ruote)
    (alter ruote)))

(def app
  (-> app-routes
      (wrap-defaults (-> api-defaults
                         (assoc-in [:params :multipart] {})
                         (assoc-in [:static :resources] "static")))
      (wrap-auth-setting wrap-cognito-authorization identity)
      (wrap-auth-setting wrap-cognito-authentication wrap-cognito-mock-authn)
      (wrap-logger)))

