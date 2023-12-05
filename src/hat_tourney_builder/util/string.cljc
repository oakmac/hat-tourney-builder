(ns hat-tourney-builder.util.string
  (:require
    [clojure.string :as str]))

(defn safe-lower-case [s]
  (when (string? s)
    (str/lower-case s)))
