(ns hat-tourney-builder.core
  (:require
    [oops.core :refer [oset!]]
    ; [dumdom.core :as dumdom :refer [defcomponent]]
    [goog.functions :as gfunctions]
    [hiccups.runtime :as hiccups]
    [hat-tourney-builder.util.dom :as dom-util :refer [set-inner-html!]])
  (:require-macros
    [hiccups.core :as hiccups :refer [html]]))



; (ns myns
;   (:require-macros [hiccups.core :as hiccups :refer [html]])
;   (:require [hiccups.runtime :as hiccups]))

(hiccups/defhtml Foo1 []
  [:div
    [:a {:href "https://github.com/weavejester/hiccup"}
      "Hiccup"]])




; (defcomponent heading
;   :on-render (fn [dom-node val old-val])
;   [data]
;   [:h2 {:style {:background "#000"}} (:text data)])

; (defcomponent HatTourneyBuilder [data]
;   [:div
;     [heading (:heading data)]
;     [:p (:body data)]])

(defn get-element [el-or-id-or-selector]
  (js/document.getElementById el-or-id-or-selector))

(defn set-html! [el html-str]
  (oset! (get-element el) "innerHTML" html-str))

(defn render! []
  ; (dumdom/render
  ;   [HatTourneyBuilder {:heading {:text "Hat Tourney Builder"}
  ;                       :body "This is a web page 222"}]
  ;   (js/document.getElementById "appContainer"))

  (set-inner-html! "appContainer" (Foo1)))

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
