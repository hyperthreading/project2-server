(ns project2-server.cognito-auth
  (:require [amazonica.aws.cognitoidp :as cognito]
            [clojure.data.json :as json]
            [buddy.core.keys :refer [str->public-key]]
            ;; [buddy.sign.jwt :as jwt]
            ;; [buddy.sign.jws :as jws]
            ;; [ring.util.response :refer (response)]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.backends :as backends]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]])
  (:import com.nimbusds.jose.jwk.RSAKey
           org.bouncycastle.openssl.jcajce.JcaPEMWriter
           java.io.StringWriter))



"
sign-in
sign-up
jwt->pem
verify-token


"


;; https"//static.javadoc.io/com.nimbusds/nimbus-jose-jwt/2.21/com/nimbusds/jose/jwk/JWKSet.html:
(defn jwk->pem-str [jwk]
  ;; https://www.javadoc.io/doc/com.nimbusds/nimbus-jose-jwt/4.35
  (let [writer (new java.io.StringWriter)
        public-key (.toRSAPublicKey (. RSAKey parse (json/write-str jwk)))
        pem-writer (new org.bouncycastle.openssl.jcajce.JcaPEMWriter writer)]
    (.writeObject pem-writer public-key)
    (.flush pem-writer)
    (.toString writer)))

;; https://cognito-idp.us-east-2.amazonaws.com/us-east-2_dh4xH7cBr/.well-known/jwks.json

;; 0 - NmQMRF5/6j2uKHhadpD1iqpQaZz/dzOWqs0MQA45MNg=
(def token-0 (str->public-key "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkNA0sTsG7JyahJya7y8o\npg/l07KwILhC+D6CHZv/6BEiuoVL8soq5XRWaCuD3nKRlUlqG29Z/XTsHn/zvRu6\nel9IdAopTvIcq9YqV+G0rVWPQOESHttk3nd+HBvJIVLsPJygajB0ZcqqUZ0t2p24\nDO7FYPQtmeAWwAesBBY6J4ORnIUyEVQdmo0z1JYA+2GzFoF8TWnr/7dVTTHjkxRf\n21x5Rm+GYBxJabPBEzYvJWTQC/y+koAezVs0hC6A4y9789lPQ/8lH1lVMG2lixXB\nC19C+ysYzFImSImrzA1kUG2j0RTt1wNxqKCxFwvLup4xYw8p8+RJxg7H1wS3JriD\newIDAQAB\n-----END PUBLIC KEY-----\n"))

;; 1 - Tlrjw+dQCj1qexc8gza2MBk8K/5KdJOcUOBVkGasSXw=

(def token-1 (str->public-key "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxOiSv1d8H3omDBsPFpp4\nQ1yPpbsS7tSa5Bpk2SSBhBGlWjr/E3aMwQRA4oBBtCInARkAZZVsWoVKGNTybqpG\nSs+qBienS5ZWAWgcPH4gEAmzquMXRZ85K0T+QBe4ouKOQgUoIyOoddeA+erzAK2v\nIZa27iFVsRrI1B76vX0NLc3b+K1V/9UY3Mitckp6z6cv5R3eBRmd4s7I0QK8EZaF\nVig/2UfDnbkqOz0pR1igEqf4WQv0lzI3eF1jknzzu1ix8/DmBoW0s4ZeJEo1gx/x\n5yExL3NzeG8HZO8CcV16yJCtyiUqt2C9CjJdSsbAumRaK6xK570lKatSBuvOXSKh\noQIDAQAB\n-----END PUBLIC KEY-----\n"))

(def back (backends/jws {:secret     token-0
                         :token-name "Bearer"
                         :options    {:alg :rs256}} ))
(def back-2 (backends/jws {:secret     token-1
                           :token-name "Bearer"
                           :options    {:alg :rs256}} ))
(defn get-user-id
  [identity-map]
  (get-in identity-map [:cognito:username]))


(defn authorization-backend
  [request handler]
  (if (authenticated? request)
    (do (println (get-user-id (:identity request)) "is authorized")
        (handler request))
    {:status 403
     :body "Not authorized"}))

;;(get-in {:email "no1expman@gmail.com", :aud "3r8verin5ei14aqtkkm5avsh6p", :sub "8dda1e37-8542-4edb-9ac0-6d0710796518", :iss "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_Kyv5EWOmi", :cognito:username "hpthrd", :name "Yejun Kim", :exp 1514979937, :event_id "3c031714-f073-11e7-a487-957f256c940d", :email_verified true, :token_use "id", :auth_time 1514976337, :preferred_username "Yejun Kim", :iat 1514976337} [:cognito:username])
(defn wrap-cognito-authentication
  [route]
  (wrap-authentication route back back-2))

(defn wrap-cognito-mock-authn
  [route]
  (fn [request]
    (route (assoc-in request [:identity :cognito:username] "hpthrd"))))

(defn wrap-cognito-authorization
  [route]
  (fn [request]
    (authorization-backend request route)))

(defn sign-in
  [id password user-pool client-id]
  (let [init-res (cognito/admin-initiate-auth {:access-key "AKIAISA7GHAU6XVFJWHQ"
                                               :secret-key "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"}
                                              {:user-pool-id    user-pool
                                               :client-id       client-id
                                               :auth-flow       "ADMIN_NO_SRP_AUTH"
                                               :auth-parameters {"USERNAME" id
                                                                 "PASSWORD" password}})]
    init-res))



;; (def tokens (:authentication-result (sign-in "hpthrd" "Trx55555@@" "us-east-2_Kyv5EWOmi" "3r8verin5ei14aqtkkm5avsh6p")))
;; (def id-token (:id-token tokens))


;; (String. (jws/unsign id-token token-0 {:alg :rs256}))
;; (def handler (wrap-authentication identity back-2 back))
;; (prn (:identity (handler {:headers {"authorization" (str "Bearer " id-token)}})))


;; (jwk->pem-str (nth (get (json/read-str (slurp "jwks.json")) "keys") 0))
;; (jwk->pem-str (json/read-str (slurp "jwks.json")))
