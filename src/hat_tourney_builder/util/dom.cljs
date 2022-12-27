(ns hat-tourney-builder.util.dom
  (:require
    [goog.dom :as gdom]
    [goog.object :as gobj]))

(defn query-select [q]
  (.querySelector js/document q))

(defn query-select-all [q]
  (.querySelectorAll js/document q))

(defn safe-prevent-default
  "calls preventDefault() on a JS event if possible"
  [js-evt]
  (when (fn? (gobj/get js-evt "preventDefault"))
    (.preventDefault js-evt)))

(defn get-element
  "does it's best to grab a native DOM element from it's argument
  arg can be either:
  1) already a DOM element
  2) id of an element (getElementById)
  3) querySelector

  return nil if not able to grab the element"
  [arg]
  (let [el1 (gdom/getElement arg)]
    (if el1
      el1
      (let [el2 (.querySelector js/document arg)]
        (if el2 el2 nil)))))

(defn add-event!
  [el-or-id evt-type evt-fn]
  (.addEventListener (get-element el-or-id) evt-type (fn [js-evt]
                                                       (evt-fn js-evt))))

(defn xy-inside-element?
  [el x y]
  (let [js-box (.getBoundingClientRect el)
        left (gobj/get js-box "left")
        width (gobj/get js-box "width")
        height (gobj/get js-box "height")
        top (gobj/get js-box "top")]
    (and (>= x left)
         (< x (+ left width))
         (>= y top)
         (< y (+ top height)))))

(defn get-height
  [el]
  (-> (.getBoundingClientRect el)
      (gobj/get "height")))

(defn get-width
  [el]
  (-> (.getBoundingClientRect el)
      (gobj/get "width")))

(defn set-style-prop!
  [el prop value]
  (-> (get-element el)
      (gobj/get "style")
      (gobj/set prop value)))

(defn show-el!
  [el]
  (set-style-prop! el "display" ""))

(defn hide-el!
  [el]
  (set-style-prop! el "display" "none"))

(defn set-inner-html!
  [el html]
  (-> (get-element el)
      (gobj/set "innerHTML" html)))

(defn append-html!
  [el additional-html]
  (-> (get-element el)
      (.insertAdjacentHTML "beforeend" additional-html)))

(defn add-class!
  [el classname]
  (-> (get-element el)
      (gobj/get "classList")
      (.add classname)))

(defn remove-class!
  [el classname]
  (-> (get-element el)
      (gobj/get "classList")
      (.remove classname)))

(defn remove-element!
  [el]
  (gdom/removeNode (get-element el)))
