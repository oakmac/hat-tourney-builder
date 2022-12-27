(ns hat-tourney-builder.core
  (:require
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

(declare get-all-players-in-dom-element initial-render! random-player-id)

(defn random-player-id []
  (str "plyr-" (random-base58 10)))

(defn refresh!
  "this function gets triggered after every shadow-cljs reload"
  []
  (initial-render!))

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

(def state
  (atom
    {; :all-players (zipmap (map :id all-players-example) all-players-example)
     :all-players {}}))

(defn store-state! []
  (set-clj-to-localstorage! "project1" @state))

; (defn read-state
;   (ls-util/set-localstorage! "project1" (prn-str @state)))

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

(defn Columns [all-players]
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

(defn DragAndDropColumns [all-players]
  [:div
   [:h1 "Hat Tourney Builder"]
   [:hr]
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
   [:div#inputPlayersContainer (InputPlayersCSV)]
   [:div#dragAndDropColumnsContainer {:style "display: none;"}]])

(defn initial-render! []
  (set-inner-html! "appContainer" (html (HatTourneyBuilder))))

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
    (swap! state assoc :all-players players)

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

(defn init-sortablejs! []
  ;; Drag and Drop Columns
  (init-sortable-list! "allPlayersList" {})
  (init-sortable-list! "linkBox" {:on-add on-add-link-box})
  (init-sortable-list! "unlinkBox" {:on-add on-add-unlink-box})

  ;; FIXME: need to add sortable on teams columns
  (doseq [itm teams-cols]
    (init-sortable-list!
      (:id itm)
      {:on-add (fn [_js-evt]
                 (update-team-summary! (:id itm)))
       :on-remove (fn [_js-evt]
                    (update-team-summary! (:id itm)))})))

(defn on-click-next-step-button [_js-evt]
  ;; get players from CSV input
  (let [players-vec (get-players-from-csv-input)
        players-with-ids (map add-random-id-to-player players-vec)
        players-map (zipmap (map :id players-with-ids) players-with-ids)]

    (swap! state assoc :all-players players-map)

    (set-inner-html! "dragAndDropColumnsContainer" (html (DragAndDropColumns players-with-ids)))
    (init-sortablejs!))

  ;; toggle display
  (dom-util/hide-el! "inputPlayersContainer")
  (dom-util/show-el! "dragAndDropColumnsContainer"))

(def add-events!
  (gfunctions/once
    (fn []
      ;; player CSV input
      (add-event! "nextStepBtn" "click" on-click-next-step-button)
      (add-event! "inputPlayersTextarea" "keyup" on-change-input-players-csv))))

;; -----------------------------------------------------------------------------
;; Init

;; NOTE: this is a "run once" function
(def init!
  (gfunctions/once
    (fn []
      (timbre/info "Initialized Tourney Hat Builder 😎")
      (initial-render!)
      (add-events!))))

      ; (set-localstorage! "foo" "bar")
      ; (store-state!)

      ; (let [xxx (read-clj-from-localstorage "project1")]
      ;   (timbre/info xxx)
      ;   (timbre/info (map? xxx))))))

(.addEventListener js/window "load" init!)
