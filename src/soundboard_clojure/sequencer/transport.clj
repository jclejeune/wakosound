(ns soundboard-clojure.sequencer.transport
  (:require [soundboard-clojure.sequencer.engine :as engine]))

;; app-state doit contenir :
;;   :seq-pattern → atom du pattern state
;;   :seq-stop    → channel de stop (nil = stoppé)
;;   :play-mode   → :one-shot | :gate

(defn playing? [app-state]
  (some? (:seq-stop @app-state)))

(defn play! [app-state kit-atom on-step-fn]
  (when-not (playing? app-state)
    (let [mode    (get @app-state :play-mode :one-shot)
          stop-ch (engine/start!
                   (:seq-pattern @app-state)
                   kit-atom
                   on-step-fn
                   {:play-mode mode})]       ; ← map, plus de kwargs
      (swap! app-state assoc :seq-stop stop-ch))))

(defn stop! [app-state]
  (when (playing? app-state)
    (engine/stop! (:seq-stop @app-state))
    (swap! app-state assoc :seq-stop nil)
    (swap! (:seq-pattern @app-state) assoc :current-step 0)))

(defn set-play-mode! [app-state mode]
  (swap! app-state assoc :play-mode mode))