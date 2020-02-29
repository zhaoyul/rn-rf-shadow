(ns example.events
  (:require
   [clojure.spec.alpha :as s]
   [cljs-http.client :as http]
   [cljs.core.async :refer [go <!]]
   [example.db :as db :refer [app-db]]
   [re-frame.core :refer [reg-sub reg-fx after
                          reg-event-db
                          reg-event-fx
                          dispatch subscribe]]))

;; -- Interceptors ------------------------------------------------------------
;;
;; See https://github.com/Day8/re-frame/blob/master/docs/Interceptors.md
;;
(defn check-and-throw
  "Throw an exception if db doesn't have a valid spec."
  [spec db [event]]
  (when-not (s/valid? spec db)
    (let [explain-data (s/explain-data spec db)]
      (throw (ex-info (str "Spec check after " event " failed: " explain-data) explain-data)))))

(def validate-spec
  (if goog.DEBUG
    (after (partial check-and-throw ::db/app-db))
    []))

;; -- Handlers --------------------------------------------------------------

(reg-event-db
 :initialize-db
 (fn [_ _]
   app-db))

(reg-sub :is-timing?
         (fn [db]
           (let [time-left (get-in db [:login :time-left])]
             (not (= 0 time-left)))))

(reg-sub
 :verify-code-btn-txt
 (fn [db]
   (let [time-left (get-in db [:login :time-left])]
     (if (= 0 time-left)
       "获取验证码"
       (str  time-left "秒后重试")))))


;; 网络请求报错,通用处理
(reg-event-fx :alert-failure
              (fn [_ [_ body]]
                (prn "接口请求出错:" body)
                {:toast
                 body}))
;; 请求登陆接口
(reg-event-fx
 :login
 (fn [_ [_ m]]
   {:http {:method :post
           :headers {:rrs-user-agent "rrs-mall-live",
                     :Accept "application/json"}
           :url "https://useapptest.rrs.com/api/v1/auth/user/login"
           :opts {:json-params m}
           :on-success [:login-success]
           :on-failure [:alert-failure]}}))

;; 登陆成功
(reg-event-fx
 :login-success
 (fn [_ _]
   (prn "login successful....")))

;; 请求验证码接口
(reg-event-fx
 :veriry-code
 (fn [_ [_ m]]
   {:http {:method :post
           :url "https://useapptest.rrs.com/api/v1/login/verifycode"
           :opts {:json-params {:mobile (:phoneNum m)}}
           :on-success [:process-code]
           :on-failure [:alert-failure]}}))

;; 定时器每次触发的event处理
(reg-event-fx
 :interval-click
 (fn [{:keys [db]} [_ interval-id]]
   (assert (not (nil? interval-id)) ":interval-click 必须携带timer的id")
   (prn ":interval-click" interval-id)
   (if (zero? (get-in db [:login :time-left]))
     (dispatch [:stop-count-down interval-id])
     {:db (update-in db [:login :time-left] dec)})))

;; 停止触发器
(reg-event-fx :stop-count-down
              (fn [_ [_ interval-id]]
                (assert (not (nil? interval-id)) "停止timer必须携带timer的id")
                (prn "stop countdown....with id:" interval-id)
                {:interval {:action    :end
                            :id        interval-id}}))
;;
(reg-event-fx
 :process-code
 (fn [{:keys [db]} [_ {:keys [data code message] :as body}]]
   (if (= 1000 code)
     (do (prn [code message] ":body" body)
         {:db (update-in db [:login :time-left] (constantly data))
          :interval {:action :start
                     :id :verify-code-countdown
                     :frequency 1000
                     :event [:interval-click :verify-code-countdown]}})
     (dispatch [:toast message]))))
