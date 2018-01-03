(ns project-2-server.util
  (:require [clojure.data.json :as json]))

(defn read-json-from-stream
  [stream & options]
  (with-open [reader (io/reader stream)]
    (apply json/read reader options)))

(defn get-map-with-not-nil
  [original keys]
  (reduce #(if-let [value (get original %2)]
             (assoc %1 %2 value)
             %1)
          {}
          keys)
  [original]
  (get-map-with-not-nil original (keys original)))

