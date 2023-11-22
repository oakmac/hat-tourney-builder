(ns hat-tourney-builder.core
  (:require
    [clojure.set :as set]
    [clojure.string :as str]
    [goog.functions :as gfunctions]
    [goog.labs.format.csv :as csv]
    [hat-tourney-builder.html :as html]
    [hat-tourney-builder.util.base58 :refer [random-base58]]
    [hat-tourney-builder.util.dom :as dom-util :refer [add-event! get-element query-select-all set-inner-html!]]
    [hat-tourney-builder.util.localstorage :refer [read-clj-from-localstorage set-clj-to-localstorage!]]
    [hat-tourney-builder.util.predicates :refer [looks-like-a-link-id? looks-like-a-player-id? looks-like-a-team-id? female? male?]]
    [oops.core :refer [oget]]
    [taoensso.timbre :as timbre])
  (:require-macros
    [hiccups.core :as hiccups :refer [html]]))

;; FIXME: when we remove a team, we need to automatically move all of the players on that team
;; to unteamed

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
  [_ _ _old-state new-state]
  (let [new-tab (:active-tab new-state)]
    (case new-tab
      "PLAYERS_INPUT_TAB"
      (do (dom-util/show-el! "inputPlayersContainer")
          (dom-util/hide-el! "dragAndDropColumnsContainer")
          (dom-util/hide-el! "exportContainer"))

      "TEAM_COLUMNS_TAB"
      (do (dom-util/hide-el! "inputPlayersContainer")
          (dom-util/show-el! "dragAndDropColumnsContainer")
          (dom-util/hide-el! "exportContainer"))

      "EXPORT_TAB"
      (do (dom-util/hide-el! "inputPlayersContainer")
          (dom-util/hide-el! "dragAndDropColumnsContainer")
          (dom-util/show-el! "exportContainer"))

      (timbre/warn "Unrecogznied :active-tab value:" new-tab))))

(defonce *state
  (atom
    {:active-tab "PLAYERS_INPUT_TAB"
     :players {}
     :teams {}}))

(defn players->summary
  [players]
  {:num-males (count (filter male? players))
   :num-females (count (filter female? players))
   :total (count players)
   :avg-strength (if (zero? (count players))
                   0
                   (/ (reduce + 0 (map :strength players))
                      (count players)))})

(defn get-link-ids-in-dom-element
  "returns a collection of all link ids within a DOM element"
  [el-id]
  {:pre [(string? el-id)]}
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
  {:pre [(string? el-id)]}
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
  ([el-id]
   (get-players-in-dom-element el-id (:players @*state)))
  ([el-id all-players-map]
   (let [player-ids (get-player-ids-in-dom-element el-id)
         players (map
                   (fn [player-id]
                     (if-let [p (get all-players-map player-id)]
                       p
                       (timbre/warn "Unable to find player with id:" player-id)))
                   player-ids)]
     (remove nil? players))))

(defn linked-player? [p]
  (looks-like-a-link-id? (:link-id p)))

;; TODO: improve performance here
(defn get-link-groups-in-dom-element
  "returns a map of <link-id> ==> [<player> <player> <player> ...]"
  ([el-id]
   (get-link-groups-in-dom-element el-id (:players @*state)))
  ([el-id all-players-map]
   (let [link-ids (get-link-ids-in-dom-element el-id)
         linked-players (filter linked-player? (vals all-players-map))
         players-by-link-group (group-by :link-id linked-players)]
     (select-keys players-by-link-group link-ids))))

(defn on-add-link-box
  "fires when a player has been added to the Link Box"
  [_js-evt]
  (let [players (get-players-in-dom-element "linkBox")
        ; link-ids (map :link-id players)
        ; player-ids (map :id players)

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

(defn update-team-summary! [team-id all-players]
  (let [players (get-players-in-dom-element team-id all-players)
        summary-id (str team-id "-summary")
        summary-el (get-element summary-id)]
    (if-not summary-el
      (timbre/error "Unable to find Team Summary DOM element:" summary-id)
      (let [summary (players->summary players)]
        (set-inner-html! summary-el (html (html/TeamSummary summary)))))))

(defn update-team-summaries!
  [_ _ _old-state new-state]
  (let [teams (:teams new-state)
        all-players (:players new-state)]
    (doseq [team-id (keys teams)]
      (update-team-summary! team-id all-players))))

(defn build-teams-and-players-str [{:keys [players teams]}]
  (let [sorted-teams (sort-by :title (vals teams))
        players-by-team (group-by :team-id (vals players))
        team-strs (map
                    (fn [{:keys [team-id title]}]
                      (let [players-on-team (get players-by-team team-id)]
                        (str "=== " title "\n"
                             (str/join "\n" (->> players-on-team
                                              (map :name)
                                              sort)))))
                    sorted-teams)]
    (str/join "\n\n" team-strs)))

(defn update-data-export!
  [_ _ _old-state {:keys [active-tab] :as new-state}]
  ;; only update the Export fields if we are on the Export Tab
  (when (= active-tab "EXPORT_TAB")
    (set-inner-html! "teamsAndPlayersTextarea" (build-teams-and-players-str new-state))
    (set-inner-html! "exportEDNTextarea" (pr-str new-state))))

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

(defn init-sortable-list!
  [id {:keys [on-add on-remove]}]
  (js/window.Sortable.
    (get-element id)
    (js-obj "animation" 150
            "group" (js-obj "name" "shared")
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
      (let [player-ids (get-player-ids-in-dom-element (oget js-evt "item.id"))
            players-map (zipmap player-ids
                                (map
                                  (fn [player-id]
                                    (let [player (get-in current-state [:players player-id])]
                                      (assoc player :team-id nil
                                                    :inside-link-box? false
                                                    :inside-unlink-box? false)))
                                  player-ids))]
        (swap! *state update-in [:players] merge players-map)
        (timbre/info "Moved linked players to un-teamed list:" player-ids))

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
    ;; TODO: make this easier to read using format
    (timbre/info (str "Added linked players to team " (:title team) " (" (:team-id team) "): "
                      (str/join "," (map (fn [p] (str (:name p) " [" (:id p) "]")) linked-players))))))

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

(defn init-single-team-sortable! [team-id]
  (init-sortable-list!
    team-id
    {:on-add (fn [js-evt]
               (add-element-to-team-column js-evt team-id))}))
     ; :on-remove (fn [js-evt]
     ;              (update-team-summary! team-id))}))

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

(defn update-teams!
  "Updates the DOM with the current state of Teams"
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
                             (html (html/SingleColumn team)))
      ;; init SortableJS on the new Column
      (init-single-team-sortable! (:team-id team)))))

    ;; FIXME: remove teams from the DOM

(defn unsorted-player?
  "Is this player unsorted? ie: not on a team, not in the link or unlink box"
  [p]
  (and (not (:team-id p))
       (not (:inside-link-box? p))
       (not (:inside-unlink-box? p))))

(defn update-players-in-all-players-column!
  "Updates the DOM inside of the All Players column."
  [all-players]
  (let [players-in-dom (get-players-in-dom-element "allPlayersList" all-players)
        link-groups-in-dom (get-link-groups-in-dom-element "allPlayersList" all-players)
        unteamed-players (filter
                           (fn [p]
                             (and (nil? (:team-id p))
                                  (not (:inside-link-box? p))
                                  (not (:inside-unlink-box? p))))
                           (vals all-players))
        unlinked-players-in-dom (filter #(not (:link-id %)) players-in-dom)
        unlinked-players (filter #(not (:link-id %)) unteamed-players)
        unlinked-player-ids-in-dom (set (map :id unlinked-players-in-dom))
        linked-players (filter linked-player? unteamed-players)
        link-groups (group-by :link-id linked-players)]
    ;; build link boxes
    (doseq [[link-id players] link-groups]
      (when-not (get link-groups-in-dom link-id)
        (dom-util/append-html! "allPlayersList" (html (html/LinkedPlayersBox (sort compare-players players))))))

    ;; build solo players
    (doseq [p unlinked-players]
      (when-not (contains? unlinked-player-ids-in-dom (:id p))
        (dom-util/append-html! "allPlayersList" (html (html/PlayerBox p)))))))

;; NOTE: update-players-in-team-column! and update-players-in-all-players-column! functions could be combined
(defn update-players-in-team-column!
  [players-on-team team]
  (let [team-id (:team-id team)
        players-in-dom (get-players-in-dom-element team-id)
        link-groups-in-dom (get-link-groups-in-dom-element team-id)
        unlinked-players-in-dom (filter #(not (:link-id %)) players-in-dom)
        unlinked-players (filter #(not (:link-id %)) players-on-team)
        unlinked-player-ids-in-dom (set (map :id unlinked-players-in-dom))
        linked-players (filter linked-player? players-on-team)
        link-groups (group-by :link-id linked-players)]
    ;; build link boxes
    (doseq [[link-id players] link-groups]
      (when-not (get link-groups-in-dom link-id)
        (dom-util/append-html! team-id (html (html/LinkedPlayersBox (sort compare-players players))))))

    ;; build solo players
    (doseq [p unlinked-players]
      (when-not (contains? unlinked-player-ids-in-dom (:id p))
        (dom-util/append-html! team-id (html (html/PlayerBox p)))))))

(defn update-players!
  [new-state]
  (let [all-players (:players new-state)
        teams (vals (:teams new-state))
        players-in-link-box (filter :inside-link-box? (vals all-players))
        sorted-linked-players (sort compare-players players-in-link-box)
        players-in-unlink-box (filter :inside-unlink-box? (vals all-players))]
    (update-players-in-all-players-column! all-players)

    ;; update players in LinkBox
    (if (empty? players-in-link-box)
      (set-inner-html! "linkBox" "")
      (set-inner-html! "linkBox" (html (html/LinkedPlayersBox sorted-linked-players))))

    ;; update players in UnlinkBox
    (set-inner-html! "unlinkBox" (html (map html/PlayerBox players-in-unlink-box)))

    ;; update players in Team Columns
    (doseq [team teams]
      (let [team-id (:team-id team)
            players-on-team-in-memory (filter #(= team-id (:team-id %))
                                              (vals all-players))]
        (update-players-in-team-column! players-on-team-in-memory team)))))


(defn update-teams-and-players
  [_ _ _old-state new-state]
  ;; NOTE: order matters here. Team columns need to be updated before players
  (update-teams! new-state)
  (update-players! new-state))

(defn click-add-team-btn [_js-evt]
  (let [num-teams (-> @*state :teams count)
        new-team-id (random-team-id)
        new-team {:team-id new-team-id
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

(defn click-export-tab-btn [_js-evt]
  (swap! *state assoc :active-tab "EXPORT_TAB"))

(def add-dom-events!
  (gfunctions/once
    (fn []
      ;; player CSV input
      (add-event! "nextStepBtn" "click" on-click-next-step-button)
      (add-event! "inputPlayersTextarea" "keyup" on-change-input-players-csv)

      (add-event! "playersInputBtn" "click" click-players-input-btn)
      (add-event! "teamsSortingBtn" "click" click-teams-sorting-btn)
      (add-event! "exportTabBtn" "click" click-export-tab-btn)

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

(def app-container-el (get-element "appContainer"))

;; NOTE: this is a "run once" function
(def init!
  (gfunctions/once
    (fn []
      (timbre/info "Initialized Tourney Hat Builder 😎")
      (when-let [state-from-localstorage (read-clj-from-localstorage "project1")]
        (timbre/info "Loaded existing state from localStorage")
        (reset! *state state-from-localstorage))

      (add-watch *state ::save-to-ls on-change-state-store-ls)
      (add-watch *state ::update-active-tab update-active-tab)
      (add-watch *state ::update-teams-and-players update-teams-and-players)
      (add-watch *state ::update-team-summaries update-team-summaries!)
      (add-watch *state ::update-data-export update-data-export!)

      (set-inner-html! "appContainer" (html (html/HatTourneyBuilder)))
      (add-dom-events!)
      (init-sortablejs!)

      ;; trigger an initial render from state
      (swap! *state identity))))

(.addEventListener js/window "load" init!)
