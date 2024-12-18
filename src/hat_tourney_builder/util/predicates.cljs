(ns hat-tourney-builder.util.predicates
  (:require
   [clojure.string :as str]))

(defn looks-like-a-link-id?
  [id]
  (and (string? id)
       (str/starts-with? id "link-")))

(defn looks-like-a-player-id?
  [id]
  (and (string? id)
       (str/starts-with? id "plyr-")))

(defn looks-like-a-team-id?
  [id]
  (and (string? id)
       (str/starts-with? id "team-")))

(defn male? [player]
  (= (:sex player) "male"))

(defn female? [player]
  (= (:sex player) "female"))
