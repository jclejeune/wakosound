(ns soundboard-clojure.ui.theme
  (:import [java.awt Color Font Dimension GraphicsEnvironment]
           [javax.swing
            JButton JLabel JPanel JComboBox JSpinner JToggleButton
            JScrollPane SpinnerNumberModel BorderFactory UIManager
            SwingConstants]
           [javax.swing.border EmptyBorder LineBorder]))

;; ──────────────────────────────────────────
;; Palette
;; ──────────────────────────────────────────

(def bg       (Color. 60 63 65))
(def fg       (Color. 187 187 187))
(def btn-bg   (Color. 77 77 77))
(def btn-fg   (Color. 220 220 220))
(def btn-dark (Color. 50 50 50))
(def orange   (Color. 255 165 0))
(def red      (Color. 200 50 50))
(def status   (Color. 250 139 1))
(def border-c (Color. 100 100 100))

;; ──────────────────────────────────────────
;; Fonts système
;; ──────────────────────────────────────────

(def font-mono-sm (Font. "Monospaced" Font/BOLD 11))
(def font-mono-md (Font. "Monospaced" Font/BOLD 14))
(def font-dialog  (Font. "Dialog" Font/BOLD 13))

;; ──────────────────────────────────────────
;; Font VFD — chargée depuis resources/fonts/
;; Fallback Monospaced si le fichier est absent
;; ──────────────────────────────────────────

(defn- load-ttf [resource-path size]
  (if-let [stream (.getResourceAsStream
                   (clojure.lang.RT/baseLoader)
                   resource-path)]
    (let [font (Font/createFont Font/TRUETYPE_FONT stream)]
      (.registerFont (GraphicsEnvironment/getLocalGraphicsEnvironment) font)
      (.deriveFont font (float size)))
    (do
      (println "WARN: font not found:" resource-path "— using fallback")
      (Font. "Monospaced" Font/BOLD (int size)))))

(def font-vfd
  (load-ttf "fonts/digital7.ttf" 24))

;; ──────────────────────────────────────────
;; Helpers de construction
;; ──────────────────────────────────────────

(defn styled-btn [label]
  (doto (JButton. label)
    (.setBackground btn-bg)
    (.setForeground btn-fg)
    (.setFocusPainted false)
    (.setFocusable false)
    (.setBorder (EmptyBorder. 6 12 6 12))
    (.setOpaque true)))

(defn pad-btn [label]
  (doto (JButton. label)
    (.setBackground btn-bg)
    (.setForeground btn-fg)
    (.setFocusPainted false)
    (.setFocusable false)
    (.setFont font-dialog)
    (.setBorder (BorderFactory/createLineBorder Color/BLACK 2))
    (.setOpaque true)
    (.setPreferredSize (Dimension. 97 97))))

(defn step-btn []
  (doto (JButton.)
    (.setBackground btn-dark)
    (.setFocusPainted false)
    (.setFocusable false)
    (.setBorder (BorderFactory/createLineBorder Color/BLACK 1))
    (.setOpaque true)
    (.setPreferredSize (Dimension. 28 28))))

(defn styled-label [text]
  (doto (JLabel. text)
    (.setForeground fg)))

(defn status-label [text]
  (doto (JLabel. text)
    (.setForeground status)
    (.setFont font-mono-md)))

(defn styled-toggle [text]
  (doto (JToggleButton. text)
    (.setBackground btn-bg)
    (.setForeground btn-fg)
    (.setFocusPainted false)
    (.setOpaque true)))

(defn styled-combo [items]
  (doto (JComboBox. (into-array String items))
    (.setBackground btn-bg)
    (.setForeground btn-fg)))

(defn styled-spinner [val min max step]
  (JSpinner. (SpinnerNumberModel. val min max step)))

;; ──────────────────────────────────────────
;; Theme global UIManager
;; ──────────────────────────────────────────

(defn apply-global-theme! []
  (doseq [[k v] {"Panel.background"      bg
                 "ScrollPane.background" bg
                 "Viewport.background"   bg
                 "Button.background"     btn-bg
                 "Button.foreground"     btn-fg
                 "Label.foreground"      fg
                 "ComboBox.background"   btn-bg
                 "ComboBox.foreground"   btn-fg
                 "Spinner.background"    btn-bg
                 "Spinner.foreground"    btn-fg}]
    (UIManager/put k v)))