(ns hat-tourney-builder.core
  (:require
    [oops.core :refer [oset!]]
    ; [dumdom.core :as dumdom :refer [defcomponent]]
    [goog.functions :as gfunctions]
    [hiccups.runtime :as hiccups]
    [hat-tourney-builder.util.dom :as dom-util :refer [set-inner-html!]])
  (:require-macros
    [hiccups.core :as hiccups :refer [html]]))

(def people
  [{:id "1232341232"
    :name "Chris Oakman"
    :sex "male"
    :rank 7
    :baggage-id "888882222"}
   {:id "823723232"
    :name "Lauren Oakman"
    :sex "female"
    :rank 8
    :baggage-id "888882222"}
   {:id "2328d99f83"
    :name "Gillian Maleski"
    :sex "female"
    :rank 8}
   {:id "234383jd"
    :name "David Waters"
    :sex "male"
    :rank 9}
   {:id "djeue8d822"
    :name "Oliver Geser"
    :sex "male"
    :rank 8}
   {:id "999222822"
    :name "Sara Wise"
    :sex "female"
    :rank 8}])


; (def state
;   (atom
;     {:all-users}))



; (hiccups/defhtml ListItem [{:keys [label]}]
;   [:div.list-item
;    ; {:id id}
;    label])

; (hiccups/defhtml Column [{:keys [id]}])
(defn Foo [{:keys [id]}]
  [:div.column "column!"])

   ; {:id id}
   ; (str "column " id)])
   ; (ListItem {:label "Item 1"})
   ; (ListItem {:label "Item 2"})
   ; (ListItem {:label "Item 3"})
   ; (ListItem {:label "Item 4"})
   ; (ListItem {:label "Item 5"})])

(hiccups/defhtml HatTourneyBuilder []
  [:div
   [:h1 "Hat Tourney Builder"]
   [:hr]
   (Foo "one")
   (Foo "two")])

    ; (list
    ;   [Column {:id "col1"}]
    ;   [Column {:id "col2"}]
    ;   [Column {:id "col3"}]
    ;   [Column {:id "col4"}])]])

   ; [:div "TODO: columns"]
   ; [:div "TODO: example player boxes"]
   ; [:div "TODO: drag-to-link box?"]])

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
