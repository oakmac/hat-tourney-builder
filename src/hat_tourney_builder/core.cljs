(ns hat-tourney-builder.core
  (:require
    [goog.functions :as gfunctions]
    [hat-tourney-builder.util.dom :refer [add-event! get-element query-select-all set-inner-html!]]
    [hiccups.runtime :as hiccups]
    [oops.core :refer [oget oset!]]
    [taoensso.timbre :as timbre])
  (:require-macros
    [hiccups.core :as hiccups :refer [html]]))

;; TODO:
;; - search All Players
;; - import player list
;; - export teams list
;; - save state on page refresh, store state in localStorage

(defn compare-players
  "sort players: female first, then sort by name"
  [player-a player-b]
  (let [a-sex (:sex player-a)
        b-sex (:sex player-b)
        a-name (:name player-a)
        b-name (:name player-b)]
    (cond
      (not= a-sex b-sex) (compare a-sex b-sex)
      :else (compare a-name b-name))))

(declare get-all-players-in-dom-element render!)

(defn refresh!
  "this function gets triggered after every shadow-cljs reload"
  []
  (render!))

(def all-players
  [{:id "1232341232"
    :name "Chris Oakman"
    :sex "male"
    :strength 7
    :baggage-id "888882222"}
   {:id "823723232"
    :name "Lauren Oakman"
    :sex "female"
    :strength 8
    :baggage-id "888882222"}
   {:id "2328d99f83"
    :name "Gillian Maleski"
    :sex "female"
    :strength 8}
   {:id "234383jd"
    :name "David Waters"
    :sex "male"
    :strength 9}
   {:id "djeue8d822"
    :name "Oliver Geser"
    :sex "male"
    :strength 8}
   {:id "999222822"
    :name "Sara Wise"
    :sex "female"
    :strength 8}])

(def state
  (atom
    {:all-players (zipmap (map :id all-players) all-players)}))

(defn PlayerBox
  [{:keys [id name sex rank]}]
  [:div {:id id
         :class (str "player-box "
                     (case sex
                       "male" "sex-male"
                       "female" "sex-female"
                       ;; TODO: warn here
                       nil))}
    name])

(defn male? [player]
  (= (:sex player) "male"))

(defn female? [player]
  (= (:sex player) "female"))

(defn players->summary
  [players]
  {:num-males (count (filter male? players))
   :num-females (count (filter female? players))
   :total (count players)
   :avg-strength (if (zero? (count players))
                   0
                   (/ (reduce + 0 (map :strength players))
                      (count players)))})

(defn format-strength-number
  [n]
  (.replace (str n) #"(\.\d\d)(\d+)$" "$1"))

(defn TeamSummary
  [{:keys [avg-strength num-females num-males total]}]
  [:table
   [:tbody
    [:tr
     [:td "Total"]
     [:td total]]
    [:tr
     [:td "Females"]
     [:td num-females]]
    [:tr
     [:td "Males"]
     [:td num-males]]
    [:tr
     [:td "Avg Strength"]
     [:td (format-strength-number avg-strength)]]]])

(defn Column [{:keys [title list-id list-items]}]
  [:div.col-wrapper-outer
    [:h2 title]
    [:div {:id (str list-id "-summary")}]
    [:div {:id list-id
           :class "col-wrapper-inner"}
      (map PlayerBox list-items)]])

(def teams-cols
  [{:id "list-team1"}
   {:id "list-team2"}
   {:id "list-team3"}
   {:id "list-team4"}])

(defn Columns []
  [:div.columns-wrapper
   (Column {:list-id "allPlayersList"
            :title "All Players"
            :list-items all-players})
   (Column {:list-id "list-team1"
            :title "Team 1"
            :list-items []})
   (Column {:list-id "list-team2"
            :title "Team 2"
            :list-items []})
   (Column {:list-id "list-team3"
            :title "Team 3"
            :list-items []})
   (Column {:list-id "list-team4"
            :title "Team 4"
            :list-items []})])

(defn LinkBoxes []
  [:div {:style "display: flex; flex-direction: row;"}
    [:div {:style "margin-right: 1em;"}
      [:h2 "Link Box"]
      [:div#linkBox]]
    [:div
      [:h2 "Unlink Box"]
      [:div#unlinkBox]]])

;; TODO: do we need a group-id here?
(hiccups/defhtml LinkedPlayersBox [players]
  [:div.linked-players-box
   (map PlayerBox players)])

(hiccups/defhtml HatTourneyBuilder []
  [:div
   [:h1 "Hat Tourney Builder"]
   [:hr]
   (Columns)
   (LinkBoxes)])

(defn render! []
  (set-inner-html! "appContainer" (HatTourneyBuilder)))

(defn on-add-link-box
  "fires when a player has been added to the Link Box"
  [_js-evt]
  (let [players (get-all-players-in-dom-element "linkBox")
        sorted-players (sort compare-players players)]
    (set-inner-html! "linkBox" (LinkedPlayersBox sorted-players))))

(defn get-all-player-ids-in-dom-element
  "returns a collection of all player ids within a DOM element"
  [el-id]
  (let [selector (str "#" el-id " .player-box")
        els (query-select-all selector)
        ids (atom [])]
    (.forEach els (fn [el]
                    (swap! ids conj (oget el "id"))))
    @ids))

(defn get-all-players-in-dom-element
  "returns a collection of all players within a DOM element"
  [el-id]
  (let [player-ids (get-all-player-ids-in-dom-element el-id)
        all-players-map (:all-players @state)
        players (map
                  (fn [player-id]
                    (if-let [p (get all-players-map player-id)]
                      p
                      (timbre/warn "Unable to find player with id:" player-id)))
                  player-ids)]
    (remove nil? players)))

(defn on-add-unlink-box [_js-evt]
  (let [players (get-all-players-in-dom-element "unlinkBox")]
    (timbre/info "Unlinking" (count players) "players:" players)
    (set-inner-html! "unlinkBox" (html (map PlayerBox players)))))

(defn update-team-summary! [team-id]
  (let [players (get-all-players-in-dom-element team-id)
        summary-id (str team-id "-summary")
        summary (players->summary players)]
    (set-inner-html! summary-id (html (TeamSummary summary)))))

(defn init-sortable-list!
  [id {:keys [on-add on-remove]}]
  (js/window.Sortable.
    (get-element id)
    (js-obj "animation" 150
            "group" "shared"
            "onAdd" (when on-add on-add)
            ; "onEnd" on-end-list
            "onRemove" (when on-remove on-remove))))

(def add-events!
  (gfunctions/once
    (fn []
      (init-sortable-list! "allPlayersList" {})
      (init-sortable-list! "linkBox" {:on-add on-add-link-box})
      (init-sortable-list! "unlinkBox" {:on-add on-add-unlink-box})

      (doseq [itm teams-cols]
        (init-sortable-list!
          (:id itm)
          {:on-add (fn [_js-evt]
                     (update-team-summary! (:id itm)))
           :on-remove (fn [_js-evt]
                        (update-team-summary! (:id itm)))})))))

;; -----------------------------------------------------
;; Init

;; NOTE: this is a "run once" function
(def init!
  (gfunctions/once
    (fn []
      (timbre/info "Initialized Tourney Hat Builder 😎")
      (render!)
      (add-events!))))

(.addEventListener js/window "load" init!)
