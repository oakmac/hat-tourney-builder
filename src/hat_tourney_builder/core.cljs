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
    ; [re-frame.core :as rf]
    ; [reagent.core :as reagent]
    ; [reagent.dom :as reagent-dom]
    [taoensso.timbre :as timbre])
  (:require-macros
    [hiccups.core :as hiccups :refer [html]]))

(declare InputPlayersCSV get-players-in-dom-element random-player-id)
(declare init-sortable-list!)

;; -----------------------------------------------------------------------------
;; Predicates

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




(defn random-link-id []
  (str "link-" (random-base58 10)))

(defn random-player-id []
  (str "plyr-" (random-base58 10)))

(defn random-team-id []
  (str "team-" (random-base58 10)))

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
  [:div {:key id
         :id id
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

(defn on-add-element-to-all-players-column
  [js-evt]
  (let [el-id (oget js-evt "item.id")]
    (timbre/info "added to all players:" el-id)))

;; FIXME: the core problem right now is that when using sortable it adjusts the DOM state
;; and then React runs and tries to sync everything and the DOM has changed so it breaks with a runtime error
; (defn on-add-element-to-team-column
;   "Fires when a DOM element is dropped on a Team Column"
;   [team-id js-evt]
;   (js/setTimeout
;     (fn []
;       (let [player-id (oget js-evt "item.id")]
;         (rf/dispatch [::add-player-to-team {:team-id team-id
;                                             :player-id player-id}])))
;     1000))

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

(declare on-add-link-box
         on-add-unlink-box)

; (defn LinkBoxes []
;   (reagent/create-class
;     {:display-name "LinkBoxes"

;      :component-did-mount
;      (fn [_this]
;        (init-sortable-list! "linkBox"
;                             {:on-add on-add-link-box})
;        (init-sortable-list! "unlinkBox"
;                             {:on-add on-add-unlink-box}))

;      :reagent-render
;      (fn []
;        [:div {:style {:display "flex", :flex-direction "row"}}
;          [:div {:style {:margin-right "1em"}}
;            [:h2 "Link Box"]
;            [:div#linkBox]]
;          [:div
;            [:h2 "Unlink Box"]
;            [:div#unlinkBox]]])}))

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

; (defn click-add-team-btn2 [_js-evt]
;   (rf/dispatch [::add-new-team]))

(defn DragAndDropColumns []
  [:div
   ; [:button#addTeamBtn {:on-click click-add-team-btn2} "Add Team"]
   [:button#addTeamBtn "Add Team"]
   ; [:button#removeColumnBtn "Remove Column"]
   (Columns)
   (LinkBoxes)])

(def example-csv-input-str
  (str "John,m,6\n"
       "Jane,f,8\n"
       "Chris,m,7\n"
       "Stephen,m,5\n"
       "Christi,f,7\n"
       "Jake,m,6\n"))

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
    (DragAndDropColumns)]])

(defn initial-render! []
  (set-inner-html! "appContainer" (html (HatTourneyBuilder))))

(defn get-link-ids-in-dom-element
  "returns a collection of all link ids within a DOM element"
  [el-id]
  (let [selector (str "#" el-id " .linked-players-box")
        els (query-select-all selector)
        ids (atom [])]
    (.forEach els (fn [el]
                    (let [el-id (oget el "id")]
                      (when (looks-like-a-link-id? el-id)
                        (swap! ids conj el-id)))))
    @ids))

(defn get-player-ids-in-dom-element
  "returns a collection of all player ids within a DOM element"
  [el-id]
  (let [selector (str "#" el-id " .player-box")
        els (query-select-all selector)
        ids (atom [])]
    (.forEach els (fn [el]
                    (let [el-id (oget el "id")]
                      (when (looks-like-a-player-id? el-id)
                        (swap! ids conj el-id)))))
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

(defn linked-player? [p]
  (looks-like-a-link-id? (:link-id p)))

;; TODO: improve performance here
(defn get-link-groups-in-dom-element
  "returns a map of <link-id> ==> [<player> <player> <player> ...]"
  [el-id]
  (let [link-ids (get-link-ids-in-dom-element el-id)
        player-ids (get-player-ids-in-dom-element el-id)
        players-map (:players @*state)
        linked-players (filter linked-player? (vals players-map))
        players-by-link-group (group-by :link-id linked-players)]
    (select-keys players-by-link-group link-ids)))

(defn on-add-link-box
  "fires when a player has been added to the Link Box"
  [_js-evt]
  (let [players (get-players-in-dom-element "linkBox")
        link-ids (map :link-id players)
        player-ids (map :id players)

        link-id (or ;; find an existing link-id
                    (->> players
                      (map :link-id)
                      (filter looks-like-a-link-id?)
                      first)
                    ;; create one
                    (random-link-id))
        linked-players (map
                         (fn [p]
                           (assoc p :link-id link-id
                                    :inside-link-box? true
                                    :inside-unlink-box? false
                                    :team-id nil))
                         players)
        linked-players-map (zipmap (map :id linked-players) linked-players)]
    (swap! *state update :players merge linked-players-map)
    (timbre/info "Players in LinkBox:"
                 (map
                   (fn [p]
                     (str (:name p) " (" (:id p) ")"))
                   linked-players))))

; (defn on-add-link-box
;   "fires when an element has been added to the Link Box"
;   [_js-evt]
;   (let [player-ids (get-player-ids-in-dom-element "linkBox")
;         linkbox-el (get-element "linkBox")
;         children-els (oget linkbox-el "children")
;         children-ids (reduce
;                        (fn [acc el]
;                          (conj acc (oget el "id")))
;                        []
;                        children-els)]
;     (timbre/info children-ids)
;     (rf/dispatch [::link-players-together player-ids])))

(defn on-add-unlink-box
  "fires when an element is added to the unlink box"
  [js-evt]
  (let [el-id (oget js-evt "item.id")
        players (get-players-in-dom-element "unlinkBox")
        unlinked-players (map
                           (fn [p]
                             (-> p
                               (assoc :inside-link-box? false)
                               (assoc :inside-unlink-box? true)
                               (assoc :team-id nil)
                               (dissoc :link-id)))
                           players)
        unlinked-players-map (zipmap (map :id unlinked-players) unlinked-players)]
    (swap! *state update :players merge unlinked-players-map)
    (timbre/info "Unlinked" (count players) "players:" players)))

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

(defn PlayerRow [{:keys [id name sex strength]}]
  [:tr {:key id}
    [:td name]
    [:td sex]
    [:td strength]])

; (defn PlayersTable
;   []
;   (let [players @(rf/subscribe [::parsed-csv-players])]
;     [:table
;       [:thead
;         [:tr
;           [:th "Name"]
;           [:th "Gender"]
;           [:th "Strength"]]]
;       [:tbody
;         (map PlayerRow players)]]))

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
         js-parsed-csv (try
                         (csv/parse csv-txt)
                         (catch js/Error _e []))
         clj-parsed (js->clj js-parsed-csv)
         clj-parsed2 (filter valid-player-row? clj-parsed)
         players-vec (map
                       (fn [row]
                         {:id (random-player-id)
                          :name (str/trim (nth row 0 nil))
                          :sex (parse-gender-str (nth row 1 nil))
                          :strength (parse-strength-num (nth row 2 nil))})
                       clj-parsed2)]
     players-vec)))

(defn on-change-input-players-csv
  [js-evt]
  (let [txt (oget js-evt "currentTarget.value")
        players (get-players-from-csv-input txt)]
    (swap! *state assoc :players players)))

    ; (set-inner-html! "parsedPlayersTable" (html (PlayersTable players)))))

(defn init-sortable-list!
  [id {:keys [on-add on-remove]}]
  (js/window.Sortable.
    (get-element id)
    (js-obj "animation" 150
            "group" (js-obj "name" "shared"
                            "pull" "clone"
                            "revertClone" true)

            "onAdd" (when on-add on-add)
            "onRemove" (when on-remove on-remove))))
            ; "removeCloneOnHide" false
            ; "pull" "clone")))
            ; "onClone" (fn [js-evt]
            ;             (let [orig-el (oget js-evt "item")
            ;                   clone-el (oget js-evt "clone")]
            ;               (dom-util/set-style-prop! orig-el "background" "red")
            ;               (dom-util/set-style-prop! clone-el "border" "5px solid green"))))))

(defn add-random-id-to-player
  [p]
  (assoc p :id (random-player-id)))

(defn link-id->player-ids
  "Returns the player-ids associated with a link-id"
  [link-id]
  ;; FIXME: write this
  nil)

(defn add-to-all-players-list
  "fires when an element is added to the All Players list"
  [js-evt]
  (let [el-id (oget js-evt "item.id")
        single-player? (looks-like-a-player-id? el-id)
        linked-players? (looks-like-a-link-id? el-id)
        current-state @*state]
    (cond
      single-player?
      (let [player-id el-id
            player (get-in current-state [:players player-id])]
        (assert player (str "Player with id not found:" player-id))
        (swap! *state update-in [:players player-id] merge {:team-id nil
                                                            :inside-link-box? false
                                                            :inside-unlink-box? false})
        (timbre/info (str "Added player " (:name player) " (" player-id ") "
                          "to un-teamed list")))

      linked-players?
      (let [link-id el-id
            player-ids (link-id->player-ids link-id)]
        (timbre/info "FIXME: handle adding linked players here"))

      :else
      (timbre/error "Unrecognized element dragged into LinkBox:" (oget js-evt "item")))))

(defn add-player-to-team!
  [player team]
  (let [player-id (:id player)
        team-id (:team-id team)]
    (assert player (str "Player with id not found:" player-id))
    (assert team (str "Team with id not found:" team-id))
    (swap! *state update-in [:players player-id] merge {:team-id team-id
                                                        :inside-link-box? false
                                                        :inside-unlink-box? false})
    (timbre/info (str "Added player " (:name player) " (" player-id ") "
                      "to team " (:title team) " (" team-id ")"))))

(defn add-linked-players-to-team!
  [linked-players team]
  (let [team-id (:team-id team)
        linked-players2 (map
                          (fn [p]
                            (assoc p :team-id team-id
                                     :inside-link-box? false
                                     :inside-unlink-box? false))
                          linked-players)
        linked-players-map (zipmap (map :id linked-players2) linked-players2)]
    (swap! *state update :players merge linked-players-map)
    (timbre/info "Added linked player group to team:" team)
    (timbre/info "Linked players:" linked-players)))

(defn add-element-to-team-column
  "this event fires when a DOM element is added to a Team column"
  [js-evt team-id]
  (let [el-id (oget js-evt "item.id")
        current-state @*state
        players (:players current-state)
        player (get players el-id)
        team (get-in current-state [:teams team-id])
        linked-players (when (looks-like-a-link-id? el-id)
                         (filter #(= el-id (:link-id %)) (vals players)))]
    (assert team (str "Unrecognized team-id: " team-id))
    (cond
      player
      (add-player-to-team! player team)

      (and linked-players (not (empty? linked-players)))
      (add-linked-players-to-team! linked-players team)

      :else
      (do (timbre/warn "Unrecognized element dropped onto Team column:")
          (js/console.log (oget js-evt "item"))
          nil))))

;; FIXME: remove "update-team-summary!", move to atom-watcher
(defn init-single-team-sortable! [team-id]
  (init-sortable-list!
    team-id
    {:on-add (fn [js-evt]
               (add-element-to-team-column js-evt team-id)
               (update-team-summary! team-id))
     :on-remove (fn [js-evt]
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

(defn unsorted-player?
  "Is this player unsorted? ie: not on a team, not in the link or unlink box"
  [p]
  (and (not (:team-id p))
       (not (:inside-link-box? p))
       (not (:inside-unlink-box? p))))

(defn update-players-in-team-column
  [players-on-team team]
  (let [team-id (:team-id team)
        players-in-dom (get-players-in-dom-element team-id)
        link-groups-in-dom (get-link-groups-in-dom-element team-id)
        unlinked-players-in-dom (filter #(not (:link-id %)) players-in-dom)
        unlinked-players (filter #(not (:link-id %)) players-on-team)
        unlinked-player-ids-in-dom (set (map :id unlinked-players-in-dom))
        linked-players (filter linked-player? players-on-team)
        link-groups (group-by :link-id linked-players)]

    ; (timbre/info "updating team:" team)
    ; (timbre/info "players:" players-on-team)
    ; (timbre/info "link groups:" link-groups)
    ; (timbre/info "-------------------------------------")

    ;; build link boxes
    (doseq [[link-id players] link-groups]
      (when-not (get link-groups-in-dom link-id)
        (dom-util/append-html! team-id (html (LinkedPlayersBox (sort compare-players players))))))

    ;; build solo players
    (doseq [p unlinked-players]
      (when-not (contains? unlinked-player-ids-in-dom (:id p))
        (dom-util/append-html! team-id (html (PlayerBox p)))))))

(defn update-players
  [new-state]
  (let [current-players (:players new-state)
        players-in-all-players-column (get-players-in-dom-element "allPlayersList")
        player-ids-in-all-players-column (set (map :id players-in-all-players-column))
        players-not-on-a-team (filter unsorted-player? (vals current-players))
        teams (vals (:teams new-state))
        players-in-link-box (filter :inside-link-box? (vals current-players))
        sorted-linked-players (sort compare-players players-in-link-box)
        players-in-unlink-box (filter :inside-unlink-box? (vals current-players))]
    ;; FIXME: this is not working correctly with linked players
    ;; fill the "All Players" column
    (doseq [player players-not-on-a-team]
      (when-not (contains? player-ids-in-all-players-column (:id player))
        (dom-util/append-html! "allPlayersList" (html (PlayerBox player)))))

    ;; FIXME: remove players from "All Players" column

    ;; update players in LinkBox
    (if (empty? players-in-link-box)
      (set-inner-html! "linkBox" "")
      (set-inner-html! "linkBox" (html (LinkedPlayersBox sorted-linked-players))))

    ;; update players in UnlinkBox
    (set-inner-html! "unlinkBox" (html (map PlayerBox players-in-unlink-box)))

    ;; update players in Team Columns
    (doseq [team teams]
      (let [team-id (:team-id team)
            players-on-team-in-memory (filter #(= team-id (:team-id %))
                                              (vals current-players))]
        (update-players-in-team-column players-on-team-in-memory team)))))

(defn update-teams-and-players
  [_ _ old-state new-state]
  ;; NOTE: order matters here. Team columns need to be updated before players
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

; (defn click-players-input-btn2 [_js-evt]
;   (rf/dispatch [::set-active-tab "PLAYERS_INPUT_TAB"]))

(defn click-teams-sorting-btn [_js-evt]
  (swap! *state assoc :active-tab "TEAM_COLUMNS_TAB"))

; (defn click-teams-sorting-btn2 [_js-evt]
;   (rf/dispatch [::set-active-tab "TEAM_COLUMNS_TAB"]))

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
;; Events

(def initial-app-db
  {:active-tab "PLAYERS_INPUT_TAB"
   :players-csv-txt example-csv-input-str
   :players {}
   :teams {}})

; ;; :init event sets the initial app-db state
; ;; NOTE: this event is called synchronously
; (rf/reg-event-db
;   :init-db
;   (fn [_ [_ initial-event]]
;     (timbre/info "Initializing app-db")
;     (if initial-event
;       (assoc initial-app-db :event initial-event)
;       initial-app-db)))

; ; (rf/reg-event-fx
; ;   :refresh-event
; ;   (fn [{:keys [db]} _]
; ;     (let [slug (get-in db [:event :slug])]
; ;       {:fetch-event {:slug slug
; ;                      :success-action [:update-event]
; ;                      :error-action [:FIXME-WRITE-THIS]}})))

; (rf/reg-event-db
;   ::set-active-tab
;   (fn [db [_ new-tab-id]]
;     (assoc db :active-tab new-tab-id)))

; (rf/reg-event-db
;   ::set-players-csv-txt
;   (fn [db [_ new-txt]]
;     (assoc db :players-csv-txt new-txt)))

; (rf/reg-event-db
;   ::set-players-from-csv-input
;   (fn [db _]
;     (let [players (-> db :players-csv-txt get-players-from-csv-input)
;           players-map (zipmap (map :id players) players)]
;       (assoc db :players players-map
;                 :active-tab "TEAM_COLUMNS_TAB"))))

; (rf/reg-event-db
;   ::add-new-team
;   (fn [db _]
;     (let [current-teams (:teams db)
;           new-team-id (random-team-id)
;           new-team {:team-id new-team-id
;                     :title (str "Team " (inc (count current-teams)))
;                     :order (inc (count current-teams))}]
;       (update db :teams assoc new-team-id new-team))))

; (rf/reg-event-db
;   ::remove-team
;   (fn [db [_ team-id]]
;     ;; FIXME: write this
;     ;; - move all players on this team to "unsorted"
;     ;; - remove the team object
;     db))

; (rf/reg-event-db
;   ::add-player-to-team
;   (fn [db [_ {:keys [player-id team-id]}]]
;     (let [player (get-in db [:players player-id])
;           team (get-in db [:teams team-id])]
;       (assert player (str "player-id not found: " player-id))
;       (assert team (str "team-id not found: " team-id))
;       (if (and player team)
;         (assoc-in db [:players player-id :team-id] team-id)
;         db))))

; (rf/reg-event-db
;   ::link-players-together
;   (fn [db [_ player-ids]]
;     (let [players (:players db)
;           ;; filter the player-ids to make sure they are valid
;           ;; and warn if otherwise
;           player-ids2 (reduce
;                         (fn [player-ids3 player-id]
;                           (if (get players player-id)
;                             (conj player-ids3 player-id)
;                             (do (timbre/warn "Unable to find player-id:" player-id)
;                                 player-ids3)))
;                         []
;                         player-ids)]
;       ;; do not create a link-id if there is only one player in the box
;       (if (= 1 (count player-ids2))
;         (update-in db [:players (first player-ids2)] merge
;                    {:link-id nil
;                     :inside-link-box? false
;                     :inside-unlink-box? false
;                     :team-id nil})
;         ;; else create a link id (or make a new one) and link all of the players
;         ;; together
;         (let [linked-players (select-keys players player-ids2)
;               link-ids (filter looks-like-a-link-id? (map :link-id linked-players))
;               link-id (or (first link-ids) (random-link-id))
;               linked-players2 (map-indexed
;                                 (fn [idx player-id]
;                                   (let [p (get players player-id)]
;                                     (assoc p :link-order (inc idx)
;                                              :link-id link-id
;                                              :inside-link-box? true
;                                              :inside-unlink-box? false
;                                              :team-id nil)))
;                                 player-ids2)
;               linked-players2-map (zipmap player-ids2 linked-players2)]
;           (update db :players merge linked-players2-map))))))

;     ; (let [current-teams (:teams db)
;     ;       new-team-id (random-team-id)
;     ;       new-team {:team-id new-team-id
;     ;                 :title (str "Team " (inc (count current-teams)))
;     ;                 :order (inc (count current-teams))}]
;     ;   (update db :teams assoc new-team-id new-team))))

;; -----------------------------------------------------------------------------
;; Subscriptions

; (rf/reg-sub
;   ::active-tab
;   (fn [db _]
;     (:active-tab db)))

; (rf/reg-sub
;   ::players-csv-txt
;   (fn [db _]
;     (:players-csv-txt db)))

; (rf/reg-sub
;   :event
;   (fn [db _]
;     (:event db)))

; (rf/reg-sub
;   ::parsed-csv-players
;   (fn [db _]
;     (-> db :players-csv-txt get-players-from-csv-input)))

; (rf/reg-sub
;   ::unteamed-players
;   (fn [db _]
;     (->> db
;       :players
;       vals
;       (filter unsorted-player?)
;       (sort compare-players))))

; (rf/reg-sub
;   ::sorted-teams
;   (fn [db _]
;     (->> db
;       :teams
;       vals
;       (sort-by :order))))

; ;; FIXME: this does not take into account LinkedGroups
; (rf/reg-sub
;   ::players-on-team
;   (fn [db [_ team-id]]
;     (->> db
;       :players
;       vals
;       (filter #(= team-id (:team-id %)))
;       (sort compare-players))))

;; -----------------------------------------------------------------------------
;; Views

; (defn click-next-step-btn2 [_js-evt]
;   (rf/dispatch [::set-players-from-csv-input]))

(defn InputPlayersCSV []
  [:div
   [:h1 "Input Players"]
   [:hr]
   ; [:button#nextStepBtn {:on-click click-next-step-btn} "Go to next step"]
   [:button#nextStepBtn "Go to next step"]
   [:br] [:br]
   [:div {:style "display: flex; flex-direction: row;"}
    [:div {:style "flex 1; padding: 8px 16px"}
     [:h4 "Enter as CSV: Name, Sex, Strength"]
     [:p "One player per row. Separate with commas: Name, Sex, Strength"]
     [:textarea#inputPlayersTextarea
       {; :on-change #(rf/dispatch [::set-players-csv-txt (oget % "currentTarget.value")])
        :style "width: 100%; min-height: 400px"
        :value "FIXME"}]]
    [:div {:style "flex: 1; padding: 8px 16px"}
     [:h4 "Parsed Players"]
     [:div#parsedPlayersTable
      ; [PlayersTable]
      "FIXME: players table"]]]])

; (defn HatTourneyBuilder2
;   []
;   (let [active-tab @(rf/subscribe [::active-tab])]
;     [:<>
;      [:h1 "Hat Tourney Builder"]
;      [:hr]
;      [:button#playersInputBtn {:on-click click-players-input-btn2} "Players Input"]
;      [:button#teamsSortingBtn {:on-click click-teams-sorting-btn2} "Teams Sorting"]
;      [:br] [:br]
;      (when (= active-tab "PLAYERS_INPUT_TAB")
;        [:div#inputPlayersContainer [InputPlayersCSV]])
;      (when (= active-tab "TEAM_COLUMNS_TAB")
;        [DragAndDropColumns])]))

;; -----------------------------------------------------------------------------
;; Init

; (defn refresh!
;   "this function gets triggered after every shadow-cljs reload"
;   []
;   (swap! *state identity))

(defn refresh!
  "this function gets triggered after every shadow-cljs reload"
  []
  nil)
  ; (rf/clear-subscription-cache!)
  ; (reagent-dom/force-update-all))

(def app-container-el (get-element "appContainer"))

; (def start-rendering!
;   (gfunctions/once
;     (fn []
;       (timbre/info "Begin rendering")
;       (reagent-dom/render [(var HatTourneyBuilder2)] app-container-el))))

; (def init!
;   "Global application init.
;   Note: this function may only be called once."
;   (gfunctions/once
;     (fn []
;       (timbre/info "Initializing Hat Tourney Builder 😎")
;       (if-not app-container-el
;         (timbre/fatal "<div id=appContainer> element not found")
; ;       (when-let [state-from-localstorage (read-clj-from-localstorage "project1")]
; ;         (timbre/info "Loaded existing state from localStorage")
; ;         (reset! *state state-from-localstorage))
;         (let [] ;js-initial-event (oget+ js/window "ULTIMATE1.?initialEvent")]
;               ; initial-event (js->clj js-initial-event :keywordize-keys true)]
;           (rf/dispatch-sync [:init-db])
;           (start-rendering!))))))
;           ; (routing/init!)
;           ; (start-polling-for-updates!))))))

; ;; NOTE: this is a "run once" function
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
