(ns hat-tourney-builder.core
  (:require
    [oops.core :refer [oset!]]
    ; [dumdom.core :as dumdom :refer [defcomponent]]
    [goog.functions :as gfunctions]
    [hiccups.runtime :as hiccups]
    [hat-tourney-builder.util.dom :as dom-util :refer [set-inner-html!]])
  (:require-macros
    [hiccups.core :as hiccups :refer [html]]))

(hiccups/defhtml HatTourneyBuilder []
  [:div
   [:h1 "Hat Tourney Builder"]
   [:hr]
   [:div "TODO: columns"]
   [:div "TODO: example player boxes"]
   [:div "TODO: drag-to-link box?"]])

(defn render! []
  (set-inner-html! "appContainer" (HatTourneyBuilder)))

;; -----------------------------------------------------
;; Init

;; NOTE: this is a "run once" function
(def init!
  (gfunctions/once
    (fn []
      (js/console.log "Initialized Tourney Hat Builder 😎")
      (render!))))

(.addEventListener js/window "load" init!)
