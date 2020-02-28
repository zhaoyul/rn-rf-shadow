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



(let [{:keys [width height ]}
      (js->clj (.get rn/Dimensions "window") :keywordize-keys true) ]
  [width height])

(def isX
  (let [{:keys [width height ]}
        (js->clj (.get rn/Dimensions "window") :keywordize-keys true) ]
    (and (> height (* 2 width))
         (= "ios" (.-OS rn/Platform)))))

(def styles
  ^js (-> {:loginBg
           {:width "100%"
            :height "100%"}
           :content
           {:flex 1
            :paddingLeft 30,
            :paddingRight 30
            :paddingTop (if isX  (+ 70 80) (+ 30  80))
            :backgroundColor "transparent"}
           :t1 {:fontSize 22
                :color "white"
                :fontWeight "bold"}
           :username_input_layout
           {:width "100%"
            :height 40,
            :marginTop 40,
            :flexDirection "row"
            :alignItems "center"}
           :t2 {:width 70,
                :fontSize 15,
                :color "white"}
           :input_username
           {:flex 1,
            :fontSize 15,
            :color "white",
            :paddingBottom 0, 
            :paddingTop 0}
           :line {:marginTop 5,
                  :marginBottom 5,
                  :height (.-hairlineWidth rn/StyleSheet)
                  :width "100%"
                  :backgroundColor "lightgray"
                  }
           :passwd_input_layout
           {
            qqwidth: '100%',
            height: 30,
            marginTop: 30,
            flexDirection: 'row',
            alignItems: 'center'
            }
           }
          (clj->js)
          (rn/StyleSheet.create)))

(def phoneNo (r/atom ""))

(defn root []
  [:> rn/ImageBackground
   {:resizeMode  "cover"
    :style (.-loginBg styles)
    :source (js/require "../assets/login-bg.jpg")}
   [:> rn/View {:style (.-content styles)}
    [:> rn/Text {:style (.-t1 styles)} (str "欢迎使用日日顺")]
    [:> rn/View {:style (.-username_input_layout styles)}
     [:> rn/Text {:style (.-t2 styles)} "+86"]
     [:> rn/TextInput {:style (.-input_username styles)
                       :placeholderTextColor "lightgray"
                       :placeholder "请输入手机号"
                       :keyboardType "numeric"
                       :returnKeyType "next"
                       :underlineColorAndroid="#00000000"
                       :onChangeText (fn [text]
                                       (prn text)
                                       (reset! phoneNo text))}]]
    [:> rn/View {:style (.-line styles)}]
    [:> rn/View {:style (.-passwd_input_layout styles)}]]
   #_[:> rn/View {:style (.-container styles)}
      [:> rn/Text {:style (.-title styles)} "Clicked: " @counter]
      [:> rn/TouchableOpacity {:style (.-button styles)
                               :on-press #(rf/dispatch [:inc-counter])}
       [:> rn/Text {:style (.-buttonText styles)} "Click me, I'll count"]]
      [:> rn/Image {:source splash-img :style {:width 200 :height 200}}]
      [:> rn/Text {:style (.-label styles)} "Using: shadow-cljs+expo+reagent+re-frame"]]])

(defn start
  {:dev/after-load true}
  []
  (expo/render-root (r/as-element [root])))

(defn init []
  (rf/dispatch-sync [:initialize-db])
  (start))

