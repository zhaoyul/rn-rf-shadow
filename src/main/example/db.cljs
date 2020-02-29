(ns example.db
  (:require [clojure.spec.alpha :as s]))

;; spec of app-db

(s/def ::time-left int?)

(s/def ::is-timing boolean?)

(s/def ::login (s/keys :req-un [::time-left
                                ::is-timing?]))

(s/def ::app-db
  (s/keys :req-un [::login]))





;; initial state of app-db
(defonce app-db {:login {:time-left 0
                         :is-timing? false}})
