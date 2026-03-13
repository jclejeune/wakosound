(ns soundboard-clojure.model.kit
  (:require [soundboard-clojure.model.pad :as pad]))

;; ──────────────────────────────────────────
;; Un kit = une map avec une liste de pads
;; Max 9 pads (grille 3x3)
;; ──────────────────────────────────────────

(def max-pads 9)

(defn make-kit
  "Construit un kit depuis une map de config (ex: depuis kits.edn)."
  [{:keys [name description pads]
    :or   {description "" pads []}}]
  {:name        name
   :description description
   :pads        (mapv pad/make-pad (take max-pads pads))})

;; ──────────────────────────────────────────
;; Fonctions utilitaires (toutes pures)
;; ──────────────────────────────────────────

(defn full?  [kit] (= max-pads (count (:pads kit))))
(defn empty-kit? [kit] (empty? (:pads kit)))
(defn pad-count [kit] (count (:pads kit)))

(defn get-pad [kit idx]
  (get (:pads kit) idx))

(defn add-pad [kit pad]
  (when-not (full? kit)
    (update kit :pads conj pad)))

(defn replace-pad [kit idx new-pad]
  (assoc-in kit [:pads idx] new-pad))

(defn remove-pad [kit idx]
  (update kit :pads
          (fn [pads] (vec (keep-indexed #(when (not= %1 idx) %2) pads)))))