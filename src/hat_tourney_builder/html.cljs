(ns hat-tourney-builder.html
  (:require
    [hiccups.runtime :as hiccups]
    [taoensso.timbre :as timbre]
    [hat-tourney-builder.util.predicates :refer [looks-like-a-link-id? looks-like-a-player-id? looks-like-a-team-id?]])
  (:require-macros
    [hiccups.core :as hiccups :refer [html]]))

;; -----------------------------------------------------------------------------
;; Helpers

(defn- format-strength-number
  [n]
  (if (= 0 n)
    "-"
    (.toFixed n 1)))

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
  [:table.table.is-fullwidth.is-narrow.tbl-725ca
   [:tbody
    [:tr
     [:td "Total"]
     [:td.unit-2dd1a [:strong total]]]
    [:tr
     [:td "Females"]
     [:td.unit-2dd1a [:strong num-females]]]
    [:tr
     [:td "Males"]
     [:td.unit-2dd1a [:strong num-males]]]
    [:tr
     [:td "Avg Strength"]
     [:td.unit-2dd1a [:strong (format-strength-number avg-strength)]]]]])

(defn SingleColumn
  [{:keys [all-players-column? players team-column? team-id title]}]
  [:div.column.column-87ea2
    [:h4.title.is-4 {:style "margin-bottom: 1rem;"} title]
    [:div.block {:id (str team-id "-summary")}]
    [:div {:id team-id
           :class "team-column col-wrapper-inner"}
      (doall (map PlayerBox players))]])

(defn Columns []
  [:div#columnsContainer.columns
   (SingleColumn {:team-id "allPlayersList"
                  :title "All Players"
                  :players [] ; unteamed-players
                  :all-players-column? true})])

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
   [:button#parseCSVBtn.button.is-primary "Parse CSV"]
   [:br] [:br]
   [:div {:style "display: flex; flex-direction: row;"}
    [:div {:style "flex 1; padding: 8px 16px"}
     [:h4 "Enter as CSV: Name, Sex, Strength"]
     [:p "One player per row. Separate with commas: Name, Sex, Strength"]
     [:textarea#inputPlayersTextarea
       {:style "width: 100%; min-height: 400px"}
       example-csv-input-str]]
    [:div {:style "flex: 1; padding: 8px 16px"}
     [:button#destroyAllPlayersBtn.button.is-danger "Destroy All Players"]
     [:h4 "Current Players"]
     [:div#currentPlayersTable]]]])

(defn DragAndDropColumns []
  [:div
   [:div.block
     [:button#addTeamBtn.button.is-primary "Add Team"]]
   ; [:button#removeColumnBtn "Remove Column"]
   (Columns)
   (LinkBoxes)])

(defn TopPageTabs []
  [:div.tabs.is-boxed
    [:ul#topPageTabsList
      [:li#playersInputTabListElement
        [:a#playersInputBtn {:href "#"} "Players Input"]]
      [:li#teamsSortingTabListElement
        [:a#teamsSortingBtn {:href "#"} "Teams Sorting"]]
      [:li#exportPlayersTabListElement
        [:a#exportTabBtn {:href "#"} "Data Export"]]]])

(defn HatTourneyBuilder []
  [:section.section
   [:h1.title "Hat Tourney Builder"]
   (TopPageTabs)
   [:div#inputPlayersContainer {:style "display: none;"}
    (InputPlayersCSV)]
   [:div#dragAndDropColumnsContainer {:style "display: none;"}
    (DragAndDropColumns)]
   [:div#exportContainer {:style "display: none;"}
    [:h2 "Teams and Players"]
    [:textarea#teamsAndPlayersTextarea {:style "height: 800px; width: 600px;"}]
    [:h2 "Raw EDN"]
    [:textarea#exportEDNTextarea {:style "height: 800px; width: 600px;"}]]])
