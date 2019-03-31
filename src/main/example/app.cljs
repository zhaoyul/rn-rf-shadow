(ns example.app
  (:require
   ["expo" :as ex]
   ["react-native" :as rn]
   ["react" :as react]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [shadow.expo :as expo]
   [example.events]
   [example.subs]))

;; must use defonce and must refresh full app so metro can fill these in
;; at live-reload time `require` does not exist and will cause errors
;; must use path relative to :output-dir

(defonce splash-img (js/require "../assets/shadow-cljs.png"))

(def styles
  ^js (-> {:container
           {:backgroundColor "#fff"}
           :label
           {:fontWeight "normal"
            :fontSize 15
            :color "blue"}}
          (clj->js)
          (rn/StyleSheet.create)))

(defn dragger []
  (let [counter @(rf/subscribe [:get-counter])
        state (r/atom {:dragger-coords {:left 100 :top 100}
                       :dragger-old-coords {:left 100 :top 100}})
        responder (.create
                   rn/PanResponder
                   #js {:onStartShouldSetPanResponder (fn [_ _] true)
                        :onMoveShouldSetPanResponder (fn [_ _] true)
                        :onPanResponerGrant (fn [_ _] true)
                        :onPanResponderMove
                        (fn [_ gesture]
                          (let [g (js->clj gesture :keywordize-keys true)
                                {:keys [dx dy]} g
                                {:keys [left top]} (:dragger-old-coords @state)]
                            (rf/dispatch [:inc-counter])
                            (swap! state assoc :dragger-coords {:left (+ left dx) :top (+ top dy)})))
                        :onPanResponderRelease
                        (fn [_ _]
                          (swap! state assoc :dragger-old-coords (:dragger-coords @state)))})
        pan-handlers (:panHandlers (js->clj responder :keywordize-keys true))]
    (fn []
      [:> rn/View (merge pan-handlers
                         {:style
                          (merge (:dragger-coords @state)
                                 {:backgroundColor "red"
                                  :width 50
                                  :height 50
                                  :borderRadius 25})})])))

(defn root []
  (let [counter (rf/subscribe [:get-counter])]
    (fn []
      [:> rn/View {:top 50 :left 50}
       [:> rn/Text {:style (.-label styles)}
        @(rf/subscribe [:get-counter])]
       [:> rn/View {:style (.-container styles)}
        [dragger]]])))

(defn start
  {:dev/after-load true}
  []
  (expo/render-root (r/as-element [root])))

(defn init []
  (rf/dispatch-sync [:initialize-db])
  (start))

