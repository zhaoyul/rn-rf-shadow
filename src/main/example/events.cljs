(ns example.events
  (:require
   [re-frame.core :refer [reg-sub reg-fx reg-event-db after reg-event-fx dispatch
                          subscribe]]
   [clojure.spec.alpha :as s]
   [cljs-http.client :as http]
   [cljs.core.async :refer [go <!]]
   [example.db :as db :refer [app-db]]))

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



(reg-event-fx :alert-failure
              (fn [_ [_ body]]
                {:toast
                 body}))
(reg-event-fx
 :login
 (fn [_ [_ m]]
   {:http {:method :post
           :url "https://useapptest.rrs.com/api/v1/auth/user/login"
           :opts {:json-params m}
           :on-success [:login-success]
           :on-failure [:alert-failure]}}))


(reg-event-fx
 :login-success
 (fn [_ _]
   (prn "login successful....")))


(reg-event-fx
 :veriry-code
 (fn [_ [_ m]]
   {:http {:method :post
           :url "https://useapptest.rrs.com/api/v1/login/verifycode"
           :opts {:json-params {:mobile (:phoneNum m)}}
           :on-success [:process-code]
           :on-failure [:alert-failure]}}))

(reg-event-fx
 :interval-click
 (fn [{:keys [db]} _ inverval-id]
   (prn ":interval-click" inverval-id)
   (if (zero? (get-in db [:login :time-left]))
     (dispatch [:stop-count-down inverval-id])
     {:db (update-in db [:login :time-left] dec)})

   ))
(reg-event-fx :stop-count-down
              (fn [_ [_ interval-id]]
                (prn "stop countdown....with id:" interval-id)
                {:interval {:action    :end
                            :id        interval-id}}))

(reg-event-fx
 :process-code
 (fn [{:keys [db]} [_ {:keys [data code message] :as body}]]
   (if (= 1000 code)
     (do (prn [code message] ":body" body)
         {:db (update-in db [:login :time-left] (constantly 5))
          :interval {:action :start
                     :id :verify-code-countdown
                     :frequency 1000
                     :event [:interval-click :verify-code-countdown]}})
     (dispatch [:toast ]))))
