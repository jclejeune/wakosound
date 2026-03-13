(ns soundboard-clojure.model.kit-manager
  (:require [clojure.edn     :as edn]
            [clojure.java.io :as io]
            [soundboard-clojure.model.kit :as kit]))

;; ──────────────────────────────────────────
;; Chargement depuis EDN
;; ──────────────────────────────────────────

(defn load-config []
  (-> (io/resource "kits/kits.edn")
      slurp
      edn/read-string))

(defn build-kits-state
  "Retourne le state initial des kits :
   {:kits {:default {...} :metal {...}}
    :current-kit :default}"
  []
  (let [config      (load-config)
        default-key (:default-kit config)]
    {:kits        (update-vals (:kits config) kit/make-kit)
     :current-kit default-key}))

;; ──────────────────────────────────────────
;; Accès et navigation (fonctions pures)
;; ──────────────────────────────────────────

(defn current-kit [state]
  (get-in state [:kits (:current-kit state)]))

(defn kit-keys [state]
  (keys (:kits state)))

(defn kit-names [state]
  (map #(get-in state [:kits % :name]) (kit-keys state)))

(defn switch-kit
  "Change le kit courant. No-op si la clé n'existe pas."
  [state kit-key]
  (if (contains? (:kits state) kit-key)
    (assoc state :current-kit kit-key)
    state))

(defn switch-kit-by-name
  "Change le kit courant par son nom (string)."
  [state kit-name]
  (let [entry (->> (:kits state)
                   (filter (fn [[_ v]] (= (:name v) kit-name)))
                   first)]
    (if entry
      (assoc state :current-kit (key entry))
      state)))