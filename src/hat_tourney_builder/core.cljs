(ns hat-tourney-builder.core
  (:require
    [clojure.set :as set]
    [clojure.string :as str]
    [goog.functions :as gfunctions]
    [goog.labs.format.csv :as csv]
    [hat-tourney-builder.util.base58 :refer [random-base58]]
    [hat-tourney-builder.util.dom :as dom-util :refer [add-event! get-element query-select-all set-inner-html!]]
    [hat-tourney-builder.util.localstorage :refer [read-clj-from-localstorage set-clj-to-localstorage!]]
    [hiccups.runtime :as hiccups]
    [oops.core :refer [oget oset!]]
    [taoensso.timbre :as timbre])
  (:require-macros
    [hiccups.core :as hiccups :refer [html]]))

(declare get-players-in-dom-element random-player-id)

;; -----------------------------------------------------------------------------
;; Predicates

(defn looks-like-a-team-id?
  [id]
  (and (string? id)
       (str/starts-with? id "team-")))

(defn male? [player]
  (= (:sex player) "male"))

(defn female? [player]
  (= (:sex player) "female"))






(defn random-team-id []
  (str "team-" (random-base58 10)))

(defn random-player-id []
  (str "plyr-" (random-base58 10)))

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

(def all-players-example
  [{:id (random-player-id)
    :name "Chris Oakman"
    :sex "male"
    :strength 7}
   {:id (random-player-id)
    :name "Lauren Oakman"
    :sex "female"
    :strength 8}
   {:id (random-player-id)
    :name "Gillian Maleski"
    :sex "female"
    :strength 8}
   {:id (random-player-id)
    :name "David Waters"
    :sex "male"
    :strength 9}
   {:id (random-player-id)
    :name "Oliver Geser"
    :sex "male"
    :strength 8}
   {:id (random-player-id)
    :name "Sara Wise"
    :sex "female"
    :strength 8}])

(defn on-change-state-store-ls
  [_ _ _old-state new-state]
  (set-clj-to-localstorage! "project1" new-state))

(defn update-active-tab
  "show / hide pages based on active-tab"
  [_ _ old-state new-state]
  (let [; old-tab (:active-tab old-state)
        new-tab (:active-tab new-state)]
    (case new-tab
      "PLAYERS_INPUT_TAB"
      (do (dom-util/hide-el! "dragAndDropColumnsContainer")
          (dom-util/show-el! "inputPlayersContainer"))

      "TEAM_COLUMNS_TAB"
      (do (dom-util/hide-el! "inputPlayersContainer")
          (dom-util/show-el! "dragAndDropColumnsContainer"))

      (timbre/warn "Unrecogznied :active-tab value:" new-tab))))

(defonce *state
  (atom
    {:active-tab "PLAYERS_INPUT_TAB"
     :players {}
     :teams {}}))

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
  (-> (.toFixed n 2)
    (.replace #"\.00$" "")
    (.replace #"0$" "")))

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

(defn SingleColumn [{:keys [players team-id title]}]
  [:div.col-wrapper-outer
    [:h2 title]
    [:div {:id (str team-id "-summary")}]
    [:div {:id team-id
           :class "team-column col-wrapper-inner"}
      (map PlayerBox players)]])

(defn Columns [all-players]
  [:div#columnsContainer.columns-wrapper
   (SingleColumn {:team-id "allPlayersList"
                  :title "All Players"
                  :players all-players})])

(defn LinkBoxes []
  [:div {:style "display: flex; flex-direction: row;"}
    [:div {:style "margin-right: 1em;"}
      [:h2 "Link Box"]
      [:div#linkBox]]
    [:div
      [:h2 "Unlink Box"]
      [:div#unlinkBox]]])

(defn LinkedPlayersBox [players]
  [:div.linked-players-box
   (map PlayerBox players)])

(defn DragAndDropColumns [all-players]
  [:div
   [:button#addTeamBtn "Add Team"]
   ; [:button#removeColumnBtn "Remove Column"]
   (Columns all-players)
   (LinkBoxes)])

(def example-csv-input-str
  (str "John,m,6\n"
       "Jane,f,8\n"
       "Chris,m,7\n"
       "Stephen,m,5\n"
       "Christi,f,7\n"
       "Jake,m,6\n"))

(defn InputPlayersCSV []
  [:div
   [:h1 "Input Players"]
   [:hr]
   [:button#nextStepBtn "Go to next step"]
   [:br] [:br]
   [:div {:style "display: flex; flex-direction: row;"}
    [:div {:style "border: 1px solid red; flex: 1;"}
     [:h4 "Enter as CSV: Name, Sex, Strength"]
     [:p "One player per row. Separate with commas: Name, Sex, Strength"]
     [:textarea#inputPlayersTextarea
       {:style "width: 100%; min-height: 400px;"}
       example-csv-input-str]]
    [:div {:style "border: 1px solid red; flex: 1;"}
     [:h4 "Parsed Players"]
     [:div#parsedPlayersTable]]]])

(defn HatTourneyBuilder []
  [:div
   [:h1 "Hat Tourney Builder"]
   [:hr]
   [:button#playersInputBtn "Players Input"]
   [:button#teamsSortingBtn "Teams Sorting"]
   [:br] [:br]
   [:div#inputPlayersContainer {:style "display: none;"}
    (InputPlayersCSV)]
   [:div#dragAndDropColumnsContainer {:style "display: none;"}
    (DragAndDropColumns [])]])

(defn initial-render! []
  (set-inner-html! "appContainer" (html (HatTourneyBuilder))))

(defn on-add-link-box
  "fires when a player has been added to the Link Box"
  [_js-evt]
  (let [players (get-players-in-dom-element "linkBox")
        sorted-players (sort compare-players players)]
    (set-inner-html! "linkBox" (html (LinkedPlayersBox sorted-players)))))

(defn get-player-ids-in-dom-element
  "returns a collection of all player ids within a DOM element"
  [el-id]
  (let [selector (str "#" el-id " .player-box")
        els (query-select-all selector)
        ids (atom [])]
    (.forEach els (fn [el]
                    (swap! ids conj (oget el "id"))))
    @ids))

(defn get-players-in-dom-element
  "returns a collection of all players within a DOM element"
  [el-id]
  (let [player-ids (get-player-ids-in-dom-element el-id)
        players-map (:players @*state)
        players (map
                  (fn [player-id]
                    (if-let [p (get players-map player-id)]
                      p
                      (timbre/warn "Unable to find player with id:" player-id)))
                  player-ids)]
    (remove nil? players)))

(defn on-add-unlink-box [_js-evt]
  (let [players (get-players-in-dom-element "unlinkBox")]
    (timbre/info "Unlinking" (count players) "players:" players)
    (set-inner-html! "unlinkBox" (html (map PlayerBox players)))))

(defn update-team-summary! [team-id]
  (let [players (get-players-in-dom-element team-id)
        summary-id (str team-id "-summary")
        summary (players->summary players)]
    (set-inner-html! summary-id (html (TeamSummary summary)))))

(defn parse-gender-str [s1]
  (let [s2 (some-> s1 str str/trim str/lower-case)]
    (case s2
      ("m" "male") "male"
      ("f" "female") "female"
      nil)))

(defn parse-strength-num [s1]
  (let [s2 (js/parseFloat s1)]
    (if (js/isNaN s2)
      nil
      s2)))

(defn PlayerRow [{:keys [name sex strength]}]
  [:tr
    [:td name]
    [:td sex]
    [:td strength]])

(defn PlayersTable
  [players]
  [:table
    [:thead
      [:tr
        [:th "Name"]
        [:th "Gender"]
        [:th "Strength"]]]
    [:tbody
      (map PlayerRow players)]])

(defn valid-player-row? [row]
  (and (vector? row)
       (= 3 (count row))
       (every? string? row)))

(defn get-players-from-csv-input
  ([]
   (-> (get-element "inputPlayersTextarea")
       (oget "value")
       get-players-from-csv-input))
  ([csv-txt]
   (let [;; TODO: wrap this parse in try/catch
         js-parsed-csv (csv/parse csv-txt)
         clj-parsed (js->clj js-parsed-csv)
         clj-parsed2 (filter valid-player-row? clj-parsed)
         players-vec (map
                       (fn [row]
                         {:name (str/trim (nth row 0 nil))
                          :sex (parse-gender-str (nth row 1 nil))
                          :strength (parse-strength-num (nth row 2 nil))})
                       clj-parsed2)]
     players-vec)))

(defn on-change-input-players-csv
  [js-evt]
  (let [txt (oget js-evt "currentTarget.value")
        players (get-players-from-csv-input txt)]
    (swap! *state assoc :players players)

    (set-inner-html! "parsedPlayersTable" (html (PlayersTable players)))))

(defn init-sortable-list!
  [id {:keys [on-add on-remove]}]
  (js/window.Sortable.
    (get-element id)
    (js-obj "animation" 150
            "group" "shared"
            "onAdd" (when on-add on-add)
            "onRemove" (when on-remove on-remove))))

(defn add-random-id-to-player
  [p]
  (assoc p :id (random-player-id)))





(defn add-to-all-players-list
  [js-evt]
  (let [player-id (oget js-evt "item.id")
        current-state @*state
        player (get-in current-state [:players player-id])]
    (assert player (str "Player with id not found:" player-id))

    (swap! *state assoc-in [:players player-id :team-id] nil)

    (timbre/info "Added player" (:name player) "(" player-id ")"
                 "to un-teamed")))







(defn on-add-player-to-team
  "this event fires when a player is moved to a team column"
  [js-evt team-id]
  (let [player-id (oget js-evt "item.id")
        current-state @*state
        player (get-in current-state [:players player-id])
        team (get-in current-state [:teams team-id])]
    (assert player (str "Player with id not found:" player-id))
    (assert team (str "Team with id not found:" team-id))

    (swap! *state assoc-in [:players player-id :team-id] team-id)

    (timbre/info "Added player" (:name player) "(" player-id ")"
                 "to team" (:title team) "(" team-id ")")))

(defn on-remove-player-to-team
  "this event fires when a player is removed from a team column"
  [js-evt])
  ; (js/console.log "remove player:" js-evt))
  ;; TODO: maybe delete this




(defn init-single-team-sortable! [team-id]
  (init-sortable-list!
    team-id
    {:on-add (fn [js-evt]
               (on-add-player-to-team js-evt team-id)
               (update-team-summary! team-id))
     :on-remove (fn [js-evt]
                  (on-remove-player-to-team js-evt)
                  (update-team-summary! team-id))}))

(defn get-team-ids-from-dom
  "returns a set of the team-ids currently in the DOM"
  []
  (let [els (dom-util/query-select-all ".team-column")
        ids (atom #{})]
    (.forEach els
      (fn [el]
        (let [id (oget el "id")]
          (when (looks-like-a-team-id? id)
            (swap! ids conj id)))))
    @ids))

(defn update-teams
  [new-state]
  (let [current-teams (:teams new-state)
        current-team-ids (set (keys current-teams))
        team-ids-in-dom (get-team-ids-from-dom)
        team-ids-need-to-be-added (set/difference current-team-ids team-ids-in-dom)
        teams-that-need-to-be-added (select-keys current-teams team-ids-need-to-be-added)]
    ;; add teams to the DOM
    (doseq [team (vals teams-that-need-to-be-added)]
      ;; add the Column html
      (dom-util/append-html! "columnsContainer"
                             (html (SingleColumn team)))
      ;; init SortableJS on the new Column
      (init-single-team-sortable! (:team-id team)))))

    ;; FIXME: remove teams from the DOM

(defn update-players
  [new-state]
  (let [current-players (:players new-state)
        players-in-all-players-column (get-players-in-dom-element "allPlayersList")
        player-ids-in-all-players-column (set (map :id players-in-all-players-column))
        players-not-on-a-team (filter #(not (:team-id %)) (vals current-players))
        teams (vals (:teams new-state))]
    ;; fill the "All Players" column
    (doseq [player players-not-on-a-team]
      (when-not (contains? player-ids-in-all-players-column (:id player))
        (dom-util/append-html! "allPlayersList" (html (PlayerBox player)))))

    ;; FIXME: remove players from "All Players" column

    ;; update players in each team column
    ;; TODO: this is inefficient, could be more performant
    (doseq [team teams]
      (let [team-id (:team-id team)
            players-on-team-in-dom (get-players-in-dom-element (:team-id team))
            player-ids-in-dom (set (map :id players-on-team-in-dom))
            players-on-team-in-memory (filter #(= team-id (:team-id %))
                                              (vals current-players))]
        (doseq [player players-on-team-in-memory]
          (when-not (contains? player-ids-in-dom (:id player))
            (dom-util/append-html! team-id (html (PlayerBox player)))))))))

(defn update-teams-and-players
  [_ _ old-state new-state]
  (update-teams new-state)
  (update-players new-state))

(defn click-add-team-btn [_js-evt]
  (let [num-teams (-> @*state :teams count)
        new-team-id (random-team-id)
        new-team {:team-id new-team-id
                  :players []
                  :title (str "Team " (inc num-teams))}]
    ;; store new team in state
    (swap! *state assoc-in [:teams new-team-id] new-team)))

(defn on-click-next-step-button [_js-evt]
  ;; get players from CSV input
  (let [players-vec (get-players-from-csv-input)
        players-with-ids (map add-random-id-to-player players-vec)
        players-map (zipmap (map :id players-with-ids) players-with-ids)]

    (swap! *state assoc :players players-map))
  (swap! *state assoc :active-tab "TEAM_COLUMNS_TAB"))

(defn click-players-input-btn [_js-evt]
  (swap! *state assoc :active-tab "PLAYERS_INPUT_TAB"))

(defn click-teams-sorting-btn [_js-evt]
  (swap! *state assoc :active-tab "TEAM_COLUMNS_TAB"))

(def add-dom-events!
  (gfunctions/once
    (fn []
      ;; player CSV input
      (add-event! "nextStepBtn" "click" on-click-next-step-button)
      (add-event! "inputPlayersTextarea" "keyup" on-change-input-players-csv)

      (add-event! "playersInputBtn" "click" click-players-input-btn)
      (add-event! "teamsSortingBtn" "click" click-teams-sorting-btn)

      (add-event! "addTeamBtn" "click" click-add-team-btn))))

(def init-sortablejs!
  (gfunctions/once
    (fn []
      (init-sortable-list! "allPlayersList" {:on-add add-to-all-players-list})
      (init-sortable-list! "linkBox" {:on-add on-add-link-box})
      (init-sortable-list! "unlinkBox" {:on-add on-add-unlink-box}))))

;; -----------------------------------------------------------------------------
;; Init

(defn refresh!
  "this function gets triggered after every shadow-cljs reload"
  []
  (swap! *state identity))

;; NOTE: this is a "run once" function
(def init!
  (gfunctions/once
    (fn []
      (timbre/info "Initialized Tourney Hat Builder 😎")
      (when-let [state-from-localstorage (read-clj-from-localstorage "project1")]
        (timbre/info "Loaded existing state from localStorage")
        (reset! *state state-from-localstorage))

      (add-watch *state :save-to-ls on-change-state-store-ls)
      (add-watch *state :update-active-tab update-active-tab)
      (add-watch *state :update-teams-and-players update-teams-and-players)

      (set-inner-html! "appContainer" (html (HatTourneyBuilder)))
      (add-dom-events!)
      (init-sortablejs!)

      ;; trigger an initial render from state
      (swap! *state identity))))

(.addEventListener js/window "load" init!)
