(ns example.utils
  (:require [re-frame.core :refer [reg-fx reg-event-db after reg-event-fx dispatch]]
            [clojure.spec.alpha :as s]
            [cljs-http.client :as http]
            [cljs.core.async :refer [go <!]]
            [example.db :as db :refer [app-db]]))


(defn toastError [str]
  (js/alert str))


;;;;;;;;;;;;;;
;; 网络请求 ;;
;;;;;;;;;;;;;;
(reg-fx
 :http
 (fn [{:keys [url method opts on-success on-failure]}]
   (go
     (prn "opts:" opts)
     (let [http-fn (case method
                     :post http/post
                     :get http/get
                     :put http/put
                     :delete http/delete)
           res     (<! (http-fn url opts))
           {:keys [status success body]} res]
       (if success
         (dispatch (conj on-success body))
         (dispatch (conj on-failure body)))))))

;;;;;;;;;;
;; 弹窗 ;;
;;;;;;;;;;

(reg-fx
 :toast
 (fn [{:keys [msg]}]
   (toastError msg)))

;;;;;;;;;;;;
;; 计时器 ;;
;;;;;;;;;;;;

(defonce interval-handler
  (let [live-intervals (atom {})]
    (fn handler [{:keys [action id frequency event]}]
      (condp = action
        :clean   (doall
                  (map #(handler {:action :end  :id  %1}) (keys @live-intervals)))
        :start   (swap! live-intervals assoc id (js/setInterval #(dispatch event) frequency))
        :end     (do (js/clearInterval (get @live-intervals id))
                     (swap! live-intervals dissoc id))))))

;; 每次reload的时候清除现有的interval
(interval-handler {:action :clean})

(re-frame.core/reg-fx
 :interval
 interval-handler)


;; 测试

(comment
  (reg-event-fx :k-test-interval
                (fn [_ _]
                  (prn "start countdown....")
                  {:interval {:action :start
                              :id :verify-code
                              :frequency 1000
                              :event [:stop-count-down :verify-code]}}))

  (reg-event-fx :stop-count-down
                (fn [_ [_ interval-id]]
                  (prn "stop countdown....with id:" interval-id)
                  #_{:interval {:action    :end
                                :id        interval-id}}))


  (dispatch [:k-test-interval])

  )
