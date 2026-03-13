(ns soundboard-clojure.sequencer.engine
  (:require [clojure.core.async               :as async]
            [soundboard-clojure.audio.player   :as player]
            [soundboard-clojure.sequencer.pattern :as pattern]))

(defn- play-step! [pat-state kit]
  (let [step   (:current-step pat-state)
        pads   (:pads kit)
        active (pattern/active-pads-at pat-state step)]
    (into {}
          (for [pad-idx active
                :let [pad (get pads pad-idx)]
                :when (and pad (:enabled pad))]
            [pad-idx (player/play! (:file-path pad))]))))

(defn- stop-active-clips! [clips]
  (doseq [[_ clip] clips]
    (player/stop-clip! clip)))

(defn start!
  "Démarre la boucle du séquenceur avec horloge absolue.
   opts : {:play-mode :one-shot|:gate}  (défaut :one-shot)
   Retourne un channel stop-ch."
  [state-atom kit-atom on-step-fn opts]
  (let [play-mode  (get opts :play-mode :one-shot)
        stop-ch    (async/chan)
        start-time (System/currentTimeMillis)
        step-count (atom 0)]
    (async/go-loop []
      (let [interval   (pattern/step-interval-ms (:bpm @state-atom))
            target     (+ start-time (* @step-count interval))
            sleep-ms   (max 0 (- target (System/currentTimeMillis)))
            timeout-ch (async/timeout sleep-ms)
            [_ ch]     (async/alts! [stop-ch timeout-ch])]
        (when (not= ch stop-ch)
          (let [played-step (:current-step @state-atom)
                clips       (play-step! @state-atom @kit-atom)]
            (swap! state-atom pattern/advance-step)
            (swap! step-count inc)
            (on-step-fn played-step)
            (when (= play-mode :gate)
              (let [next-interval (pattern/step-interval-ms (:bpm @state-atom))]
                (future
                  (Thread/sleep next-interval)
                  (stop-active-clips! clips)))))
          (recur))))
    stop-ch))

(defn stop! [stop-ch]
  (when stop-ch
    (async/put! stop-ch :stop)))