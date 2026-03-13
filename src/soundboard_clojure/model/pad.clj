(ns soundboard-clojure.model.pad
  (:require [clojure.java.io :as io]
            [clojure.string  :as str]))

;; ──────────────────────────────────────────
;; Un pad = une map, rien de plus
;; ──────────────────────────────────────────

(def default-pad
  {:name        "Empty"
   :file-path   nil
   :color       nil
   :volume      1.0
   :enabled     true
   :description ""})

(defn make-pad
  "Construit un pad depuis une map de config (ex: depuis kits.edn)."
  [{:keys [name file color volume description]
    :or   {color nil volume 1.0 description ""}}]
  (merge default-pad
         {:name        name
          :file-path   file
          :color       color
          :volume      (-> volume (max 0.0) (min 1.0))
          :description description}))

;; ──────────────────────────────────────────
;; Fonctions utilitaires
;; ──────────────────────────────────────────

(defn display-name
  "Nom affiché : entre crochets si le pad est désactivé."
  [pad]
  (if (:enabled pad)
    (:name pad)
    (str "[" (:name pad) "]")))

(defn- audio-ext? [path]
  (boolean (re-find #"\.(wav|mp3|aiff|au)$" (str/lower-case path))))

(defn valid?
  "Vérifie que le fichier audio du pad existe et est supporté."
  [pad]
  (let [path (:file-path pad)]
    (and path
         (not (str/blank? path))
         (audio-ext? path)
         (.exists (io/file path)))))