(ns hat-tourney-builder.core
  (:require
    [dumdom.core :as dumdom :refer [defcomponent]]
    [goog.functions :as gfunctions]))

(defcomponent heading
  :on-render (fn [dom-node val old-val])
  [data]
  [:h2 {:style {:background "#000"}} (:text data)])

(defcomponent HatTourneyBuilder [data]
  [:div
    [heading (:heading data)]
    [:p (:body data)]])

(defn render! []
  (dumdom/render
    [HatTourneyBuilder {:heading {:text "Hat Tourney Builder"}
                        :body "This is a web page 222"}]
    (js/document.getElementById "appContainer")))

(defn init!* []
  (js/console.log "Initialized Tourney Hat Builder 😎")
  (render!))

;; -----------------------------------------------------
;; Init

(def init!
  (gfunctions/once
    (fn []
      (init!*))))

(.addEventListener js/window "load" init!)
