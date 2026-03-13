(ns soundboard-clojure.sampler.sampler
  (:require [soundboard-clojure.audio.player      :as player]
            [soundboard-clojure.model.kit-manager  :as km]))

;; ──────────────────────────────────────────
;; Le sampler joue les pads du kit courant
;; app-state contient :kits-state
;; ──────────────────────────────────────────

(defn play-pad!
  "Joue le pad à l'index donné dans le kit courant."
  [app-state pad-idx]
  (let [kit  (km/current-kit (:kits-state @app-state))
        pad  (get (:pads kit) pad-idx)]
    (when (and pad (:enabled pad))
      (player/play! (:file-path pad)))))