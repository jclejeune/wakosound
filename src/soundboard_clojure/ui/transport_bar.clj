(ns soundboard-clojure.ui.transport-bar
  (:require [soundboard-clojure.ui.theme :as theme])
  (:import [java.awt Dimension Font Color]
           [javax.swing
            JPanel JLabel JToggleButton
            ButtonGroup BoxLayout Box JSeparator
            SwingConstants BorderFactory]
           [javax.swing.border EmptyBorder]))

(def ^:private bar-height 40)

;; ──────────────────────────────────────────
;; Helpers
;; ──────────────────────────────────────────

(defn- fixed-panel [width & components]
  (let [p (JPanel.)]
    (.setLayout p (BoxLayout. p BoxLayout/X_AXIS))
    (doto p
      (.setBackground theme/bg)
      (.setPreferredSize (Dimension. width bar-height))
      (.setMinimumSize   (Dimension. width bar-height))
      (.setMaximumSize   (Dimension. width bar-height))
      (.setBorder (EmptyBorder. 4 5 4 5)))
    (doseq [c components] (.add p c))
    p))

(defn- separator []
  (doto (JSeparator. SwingConstants/VERTICAL)
    (.setForeground theme/border-c)
    (.setPreferredSize (Dimension. 1 (- bar-height 10)))
    (.setMaximumSize   (Dimension. 1 (- bar-height 10)))))

(defn- vspace [w] (Box/createRigidArea (Dimension. w 0)))

(defn- lbl [text]
  (doto (JLabel. text)
    (.setForeground theme/fg)
    (.setFont (Font. "Dialog" Font/PLAIN 11))))

(defn- radio-btn [label selected?]
  (doto (JToggleButton. label selected?)
    (.setBackground theme/btn-dark)
    (.setForeground theme/btn-fg)
    (.setFocusPainted false)
    (.setOpaque true)
    (.setFont (Font. "Dialog" Font/PLAIN 11))
    (.setPreferredSize (Dimension. 65 26))
    (.setMaximumSize   (Dimension. 65 26))))

(defn- lcd-label [text]
  (doto (JLabel. text SwingConstants/CENTER)
    (.setFont       theme/font-vfd)
    (.setForeground (Color. 80 220 255))
    (.setBackground (Color. 5 10 20))
    (.setOpaque true)
    (.setBorder (BorderFactory/createLineBorder (Color. 40 40 60) 1))
    (.setPreferredSize (Dimension. 48 28))
    (.setMaximumSize   (Dimension. 48 28))))

;; ──────────────────────────────────────────
;; Construction
;; ──────────────────────────────────────────

(defn make-panel []
  (let [play-btn       (doto (theme/styled-btn "▶")
                         (.setPreferredSize (Dimension. 38 28))
                         (.setMaximumSize   (Dimension. 38 28)))
        clear-btn      (doto (theme/styled-btn "⟳")
                         (.setPreferredSize (Dimension. 34 28))
                         (.setMaximumSize   (Dimension. 34 28)))
        bpm-spinner    (doto (theme/styled-spinner 120 1 999 1)
                         (.setPreferredSize (Dimension. 58 26))
                         (.setMaximumSize   (Dimension. 58 26)))
        length-spinner (doto (theme/styled-spinner 16 1 16 1)
                         (.setPreferredSize (Dimension. 46 26))
                         (.setMaximumSize   (Dimension. 46 26)))
        one-shot-btn   (radio-btn "1-Shot" true)
        gate-btn       (radio-btn "Gate"   false)
        _              (doto (ButtonGroup.)
                         (.add one-shot-btn)
                         (.add gate-btn))
        step-lcd       (lcd-label " 1")

        ;; Sections — total ~580px
        s-transport (fixed-panel 90
                                 play-btn (vspace 4) clear-btn)
        s-bpm       (fixed-panel 110
                                 (lbl "BPM") (vspace 5) bpm-spinner)
        s-steps     (fixed-panel 95
                                 (lbl "Steps") (vspace 5) length-spinner)
        s-mode      (fixed-panel 155
                                 (lbl "Mode") (vspace 5) one-shot-btn (vspace 3) gate-btn)
        s-step      (fixed-panel 90
                                 (lbl "Step") (vspace 6) step-lcd)

        bar (JPanel.)]

    (.setLayout bar (BoxLayout. bar BoxLayout/X_AXIS))
    (doto bar
      (.setBackground theme/bg)
      (.setMaximumSize (Dimension. Integer/MAX_VALUE bar-height))
      (.setBorder (BorderFactory/createMatteBorder 0 0 1 0 theme/border-c))
      (.add s-transport)
      (.add (separator))
      (.add s-bpm)
      (.add (separator))
      (.add s-steps)
      (.add (separator))
      (.add s-mode)
      (.add (separator))
      (.add s-step)
      (.add (Box/createHorizontalGlue)))

    [bar {:play-btn       play-btn
          :clear-btn      clear-btn
          :step-label     step-lcd
          :bpm-spinner    bpm-spinner
          :length-spinner length-spinner
          :one-shot-btn   one-shot-btn
          :gate-btn       gate-btn}]))

(defn set-playing-text! [play-btn playing?]
  (.setText play-btn (if playing? "⏸" "▶")))

(defn set-step-text! [step-lcd step]
  (.setText step-lcd (format "%2d" (inc step))))