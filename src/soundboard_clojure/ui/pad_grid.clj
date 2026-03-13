(ns soundboard-clojure.ui.pad-grid
  (:require [soundboard-clojure.ui.theme :as theme])
  (:import [java.awt GridLayout]
           [javax.swing JPanel SwingUtilities]))

;; ──────────────────────────────────────────
;; Grille 3x3 — mode Sampler (MPC)
;; ──────────────────────────────────────────

(defn flash! [btn]
  ;; fn au lieu de #() pour éviter les lambdas imbriqués
  (SwingUtilities/invokeLater
   (fn []
     (.setBackground btn theme/orange)
     (future
       (Thread/sleep 150)
       (SwingUtilities/invokeLater
        (fn [] (.setBackground btn theme/btn-bg)))))))

(defn make-pad-buttons []
  (mapv (fn [i] (theme/pad-btn (str "Pad " (inc i))))
        (range 9)))

(defn refresh! [pad-buttons kit]
  (SwingUtilities/invokeLater
   (fn []
     (dotimes [i 9]
       (let [btn (nth pad-buttons i)
             pad (get (:pads kit) i)]
         (if pad
           (do (.setText btn (:name pad))
               (.setEnabled btn (boolean (:enabled pad)))
               (.setBackground btn theme/btn-bg))
           (do (.setText btn "—")
               (.setEnabled btn false)
               (.setBackground btn theme/btn-dark))))))))

(defn wire-actions! [pad-buttons play-fn]
  (dotimes [i 9]
    (let [btn (nth pad-buttons i)
          idx i]
      (.addActionListener btn
                          (reify java.awt.event.ActionListener
                            (actionPerformed [_ _]
                              (play-fn idx)
                              (flash! btn)))))))

(defn make-panel [pad-buttons]
  (let [panel (doto (JPanel. (GridLayout. 3 3 2 2))
                (.setBackground theme/bg)
                (.setPreferredSize (java.awt.Dimension. 300 300)))]
    (doseq [btn pad-buttons] (.add panel btn))
    panel))