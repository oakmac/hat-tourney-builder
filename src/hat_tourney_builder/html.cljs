(ns hat-tourney-builder.html
  (:require
    [clojure.string :as str]
    [hiccups.runtime :as hiccups]
    [taoensso.timbre :as timbre]
    [hat-tourney-builder.util.predicates :refer [looks-like-a-link-id? looks-like-a-player-id? looks-like-a-team-id?]])
  (:require-macros
    [hiccups.core :as hiccups :refer [html]]))

;; -----------------------------------------------------------------------------
;; Helpers

(defn- format-strength-number
  [n]
  (-> (.toFixed n 2)
    (.replace #"\.00$" "")
    (.replace #"0$" "")))

;; -----------------------------------------------------------------------------
;; Public API

(defn PlayerBox
  [{:keys [id name sex rank]}]
  [:div {:key id
         :id id
         :class (str "player-box "
                     (case sex
                       "male" "sex-male"
                       "female" "sex-female"
                       ;; TODO: warn here
                       nil))}
    name])

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

; (defn SingleColumn
;   [{:keys [all-players-column? players team-column? team-id title]}]
;   (reagent/create-class
;     {:display-name "SingleColumn"
;      :component-did-mount
;      (fn [_this]
;        (when all-players-column?
;          (init-sortable-list! team-id
;                               {:on-add on-add-element-to-all-players-column}))
;        (when team-column?
;          (init-sortable-list! team-id
;                               {:on-add (partial on-add-element-to-team-column team-id)})))

;      :reagent-render
;      (fn [{:keys [players team-id title]}]
;        (let [players2 (or players
;                          @(rf/subscribe [::players-on-team team-id]))]
;          [:div.col-wrapper-outer
;            [:h2 title]
;            [:div {:id (str team-id "-summary")}]
;            [:div {:id team-id
;                   :class "team-column col-wrapper-inner"}
;              (doall (map PlayerBox players2))]]))}))

(defn SingleColumn
  [{:keys [all-players-column? players team-column? team-id title]}]
  [:div.col-wrapper-outer
    [:h2 title]
    [:div {:id (str team-id "-summary")}]
    [:div {:id team-id
           :class "team-column col-wrapper-inner"}
      (doall (map PlayerBox players))]])

; (defn Columns []
;   (let [unteamed-players @(rf/subscribe [::unteamed-players])
;         sorted-teams @(rf/subscribe [::sorted-teams])]
;     [:div#columnsContainer.columns-wrapper
;      [SingleColumn {:team-id "allPlayersList"
;                     :title "All Players"
;                     :players unteamed-players
;                     :all-players-column? true}]
;      (for [team sorted-teams]
;        ^{:key (:team-id team)} [SingleColumn (assoc team :team-column? true)])]))

(defn Columns []
  [:div#columnsContainer.columns-wrapper
   (SingleColumn {:team-id "allPlayersList"
                  :title "All Players"
                  :players [] ; unteamed-players
                  :all-players-column? true})])
   ; (for [team sorted-teams]
   ;   ^{:key (:team-id team)} [SingleColumn (assoc team :team-column? true)])])

(defn LinkBoxes []
  [:div {:style "display: flex; flex-direction: row"}
   [:div {:style "margin-right: 1em"}
     [:h2 "Link Box"]
     [:div#linkBox]]
   [:div
     [:h2 "Unlink Box"]
     [:div#unlinkBox]]])

(defn LinkedPlayersBox [players]
  (let [link-ids (map :link-id players)
        link-ids-set (set link-ids)]
    ;; sanity-check that all the linked players are in the same link-group
    (assert (looks-like-a-link-id? (first link-ids)))
    (assert (= 1 (count link-ids-set)))
    [:div.linked-players-box
      {:id (first link-ids-set)}
      (map PlayerBox players)]))

(defn PlayerRow [{:keys [id name sex strength]}]
  [:tr {:data-player-id id}
    [:td name]
    [:td sex]
    [:td strength]
    [:td id]
    [:td [:button {:data-player-id id} "remove"]]])

(defn PlayersTable
  [sorted-players]
  [:table
    [:thead
      [:tr
        [:th "Name"]
        [:th "Gender"]
        [:th "Strength"]
        [:th "id"]]]
    [:tbody
      (map PlayerRow sorted-players)]])

(def example-csv-input-str
  (str "John,m,6\n"
       "Jane,f,8\n"
       "Chris,m,7\n"
       "Stephen,m,5\n"
       "Christi,f,7\n"
       "Jake,m,6\n"
       "Jenn,f,4\n"
       "Roger,m,4\n"))

(defn InputPlayersCSV []
  [:div
   [:h1 "Input Players"]
   [:hr]
   [:button#parseCSVBtn "Parse CSV"]
   [:br] [:br]
   [:div {:style "display: flex; flex-direction: row;"}
    [:div {:style "flex 1; padding: 8px 16px"}
     [:h4 "Enter as CSV: Name, Sex, Strength"]
     [:p "One player per row. Separate with commas: Name, Sex, Strength"]
     [:textarea#inputPlayersTextarea
       {:style "width: 100%; min-height: 400px"}
       example-csv-input-str]]
    [:div {:style "flex: 1; padding: 8px 16px"}
     [:button#destroyAllPlayersBtn "Destroy All Players"]
     [:h4 "Current Players"]
     [:div#currentPlayersTable]]]])

(defn DragAndDropColumns []
  [:div
   [:button#addTeamBtn "Add Team"]
   ; [:button#removeColumnBtn "Remove Column"]
   (Columns)
   (LinkBoxes)])

(defn HatTourneyBuilder []
  [:div
   [:h1 "Hat Tourney Builder"]
   [:hr]
   [:button#playersInputBtn "Players Input"]
   [:button#teamsSortingBtn "Teams Sorting"]
   [:button#exportTabBtn "Data Export"]
   [:br] [:br]
   [:div#inputPlayersContainer {:style "display: none;"}
    (InputPlayersCSV)]
   [:div#dragAndDropColumnsContainer {:style "display: none;"}
    (DragAndDropColumns)]
   [:div#exportContainer {:style "display: none;"}
    [:h2 "Teams and Players"]
    [:textarea#teamsAndPlayersTextarea {:style "height: 800px; width: 600px;"}]
    [:h2 "Raw EDN"]
    [:textarea#exportEDNTextarea {:style "height: 800px; width: 600px;"}]]])
