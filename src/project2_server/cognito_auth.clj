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


;; https://static.javadoc.io/com.nimbusds/nimbus-jose-jwt/2.21/com/nimbusds/jose/jwk/JWKSet.html
(defn jwk->pem-str [jwk]
  ;; https://www.javadoc.io/doc/com.nimbusds/nimbus-jose-jwt/4.35
  (let [writer (new java.io.StringWriter)
        public-key (.toRSAPublicKey (. RSAKey parse (json/write-str jwk)))
        pem-writer (new org.bouncycastle.openssl.jcajce.JcaPEMWriter writer)]
    (.writeObject pem-writer public-key)
    (.flush pem-writer)
    (.toString writer)))



;; 0 - NmQMRF5/6j2uKHhadpD1iqpQaZz/dzOWqs0MQA45MNg=
(def token-0 (str->public-key "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2z2LugVawc4V2sZRx8SG\nutEe3X8fXAydd20ZY7DbovCZPnbamCR4mkqk1TunZp6SuAqjO+1OVdbMHamVC2aA\nQLOGUzQ+OoKZfogkmhf5oiAm0gMy6lAkqVRWyJiJ2qKL+vHdVuZgkOCo1TtfJsgO\nAu7K0RgHoFbqvLK/qKv9/38wASrz95d1NQihrogfJ3epi/Pn1lE+/Yv8hOaTezaj\nBevqIZGxvALY4s5mvYvxhGfSTeFGnE1nsIH+dE6RIsG6+bKzExNyPLBZbT9IKSMf\npf0pOQJXRJclqDdmVV9rBWSLE5nl25iJEt1V+bmYo21Ke5TA9FMH/PSNj0qRXuay\n1wIDAQAB\n-----END PUBLIC KEY-----\n"))

;; 1 - Tlrjw+dQCj1qexc8gza2MBk8K/5KdJOcUOBVkGasSXw=

(def token-1 (str->public-key "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlL/Q7+8inNX3KJtc3vRV\naLEhUi/BqIx+/0T+UovdXikAy3N6KHwDvsfFUcMvNhipNAO3qX33mYo5waK7x7+8\nAtNUNtgMCyTiHEM3TuOhrq8CbAkawLdRuvpjp0Qu7/b/GyW4L2zm/AFTWr1/Xqxt\n00x8A4JIkW6M1P5IDCpuVLTCa2Konr5M35vfqu38jJhOx8laH0IYqC4AxtUblwIU\nnC/Q42jGD4PQNWdjO19ZGzEoADrNGs7IcGTi789g3AebjpujjMbblv3SZy0z2cYM\nrEnnOWyx28M1VSOBi13FcuWPnvcLFWEE9NyP4GfC10/eIN3FsZGKP1qIJjqERKin\n9wIDAQAB\n-----END PUBLIC KEY-----\n"))

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
