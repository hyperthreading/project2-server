(defproject project2-server "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.5.1" :exclusions [commons-io]]
                 [ring/ring-defaults "0.2.1" :exclusions [commons-io]]
                 [org.clojure/data.json "0.2.6"]
                 [com.novemberain/monger "3.1.0"]
                 [amazonica "0.3.117" :exclusions [com.fasterxml.jackson.core/jackson-core org.clojure/tools.reader commons-codec]]
                 [org.clojure/data.codec "0.1.1"]
                 [buddy "2.0.0" :exclusions [com.fasterxml.jackson.dataformat/jackson-dataformat-cbor commons-codec]]
                 [com.nimbusds/nimbus-jose-jwt "4.13"]
                 [org.bouncycastle/bcprov-jdk15on "1.58"]
                 [com.cemerick/url "0.1.1"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler project2-server.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})
