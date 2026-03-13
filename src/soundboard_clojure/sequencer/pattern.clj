(ns soundboard-clojure.sequencer.pattern)

;; ──────────────────────────────────────────
;; Un pattern = vecteur 2D [pad][step] de booléens
;; Tout ici est fonctionnel pur (pas d'effets de bord)
;; ──────────────────────────────────────────

(def max-pads  9)
(def max-steps 16)

(defn empty-pattern []
  (vec (repeat max-pads (vec (repeat max-steps false)))))

(defn initial-state []
  {:pattern        (empty-pattern)
   :current-step   0
   :pattern-length 16
   :bpm            120})

;; ──────────────────────────────────────────
;; Accès
;; ──────────────────────────────────────────

(defn get-step [state pad-idx step-idx]
  (get-in state [:pattern pad-idx step-idx]))

(defn active-pads-at
  "Retourne les indices de pads actifs sur un step donné."
  [state step-idx]
  (filter #(get-step state % step-idx)
          (range max-pads)))

;; ──────────────────────────────────────────
;; Mutations (retournent un nouveau state)
;; ──────────────────────────────────────────

(defn set-step [state pad-idx step-idx active?]
  (assoc-in state [:pattern pad-idx step-idx] active?))

(defn toggle-step [state pad-idx step-idx]
  (update-in state [:pattern pad-idx step-idx] not))

(defn clear-pattern [state]
  (assoc state :pattern (empty-pattern)))

(defn clear-pad [state pad-idx]
  (assoc-in state [:pattern pad-idx] (vec (repeat max-steps false))))

(defn set-bpm [state bpm]
  (assoc state :bpm (-> bpm (max 1) (min 999))))

(defn set-pattern-length [state length]
  (assoc state :pattern-length (-> length (max 1) (min max-steps))))

(defn advance-step [state]
  (update state :current-step #(mod (inc %) (:pattern-length state))))

;; ──────────────────────────────────────────
;; Calculs
;; ──────────────────────────────────────────

(defn step-interval-ms
  "Durée d'un step en ms pour un BPM donné (double-croches)."
  [bpm]
  (long (/ (* 60 1000) (* bpm 4))))