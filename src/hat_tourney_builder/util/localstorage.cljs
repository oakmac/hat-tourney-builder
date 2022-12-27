(ns hat-tourney-builder.util.localstorage
  (:require
    [clojure.edn :as edn]))

(defn set-localstorage!
  [k v]
  (.setItem js/window.localStorage k v))

(defn read-localstorage
  [k]
  (.getItem js/window.localStorage k))

(defn set-clj-to-localstorage!
  [k clj-v]
  ;; TODO: warn here if clj-v is a string
  (set-localstorage! k (prn-str clj-v)))

(defn read-clj-from-localstorage
  [k]
  ;; TODO: wrap this in try/catch
  (let [txt (read-localstorage k)]
    (edn/read-string txt)))
