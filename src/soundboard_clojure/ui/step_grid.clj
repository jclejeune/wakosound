(ns soundboard-clojure.ui.step-grid
  (:require [soundboard-clojure.ui.theme          :as theme]
            [soundboard-clojure.sequencer.pattern :as pattern])
  (:import [java.awt Graphics Graphics2D RenderingHints Color Font Dimension]
           [java.awt.event MouseAdapter MouseEvent]
           [javax.swing JPanel JScrollPane]))

(def ^:private label-w   70)
(def ^:private header-h  22)
(def ^:private pad-count  9)
(def ^:private step-count 16)

;; Calcul dynamique depuis la taille réelle du panel
(defn- cell-w [panel-w] (/ (- panel-w label-w) step-count))
(defn- cell-h [panel-h] (/ (- panel-h header-h) pad-count))

(defn- step-at-x [x panel-w]
  (when (>= x label-w)
    (let [cw (cell-w panel-w)
          s  (int (/ (- x label-w) cw))]
      (when (< s step-count) s))))

(defn- pad-at-y [y panel-h]
  (when (>= y header-h)
    (let [ch (cell-h panel-h)
          p  (int (/ (- y header-h) ch))]
      (when (< p pad-count) p))))

(defn make-panel [kit]
  (let [state-atom (atom {:pat     (pattern/initial-state)
                          :current -1
                          :kit     kit})

        panel (proxy [JPanel] []
                (paintComponent [^Graphics g]
                  (proxy-super paintComponent g)
                  (let [^Graphics2D g2  (.create g)
                        w               (.getWidth  ^JPanel this)
                        h               (.getHeight ^JPanel this)
                        cw              (cell-w w)
                        ch              (cell-h h)
                        {:keys [pat current kit]} @state-atom
                        pads            (:pads kit)]

                    (.setRenderingHint g2
                                       RenderingHints/KEY_ANTIALIASING
                                       RenderingHints/VALUE_ANTIALIAS_ON)

                    ;; Fond
                    (.setColor g2 theme/bg)
                    (.fillRect g2 0 0 w h)

                    ;; Headers
                    (.setFont g2 (Font. "Dialog" Font/PLAIN 10))
                    (.setColor g2 theme/fg)
                    (dotimes [s step-count]
                      (let [x (int (+ label-w (* s cw)))]
                        (.drawString g2 (str (inc s))
                                     (int (+ x (/ cw 2) -4))
                                     (int (- header-h 4)))))

                    ;; Cellules
                    (dotimes [p pad-count]
                      (let [y (int (+ header-h (* p ch)))]
                        ;; Label pad
                        (.setFont g2 (Font. "Dialog" Font/PLAIN 11))
                        (.setColor g2 theme/fg)
                        (.drawString g2
                                     (or (get-in pads [p :name]) (str "Pad " (inc p)))
                                     4 (int (+ y (/ ch 2) 4)))

                        (dotimes [s step-count]
                          (let [x     (int (+ label-w (* s cw)))
                                act?  (pattern/get-step pat p s)
                                cur?  (= s current)
                                color (cond cur?  theme/red
                                            act?  theme/orange
                                            :else theme/btn-dark)]
                            (.setColor g2 color)
                            (.fillRect g2 (+ x 1) (+ y 1)
                                       (int (- cw 2)) (int (- ch 2)))
                            (.setColor g2 Color/BLACK)
                            (.drawRect g2 x y (int cw) (int ch))))))

                    (.dispose g2)))

                ;; Pas de preferredSize fixe → s'étire avec le parent
                (getPreferredSize []
                  (Dimension. 100 100)))]

    (.setBackground panel theme/bg)
    (.setDoubleBuffered panel true)

    (let [toggle-fn (atom nil)]
      (.addMouseListener panel
                         (proxy [MouseAdapter] []
                           (mousePressed [^MouseEvent e]
                             (let [w (.getWidth  ^JPanel panel)
                                   h (.getHeight ^JPanel panel)
                                   s (step-at-x (.getX e) w)
                                   p (pad-at-y  (.getY e) h)]
                               (when (and s p @toggle-fn)
                                 (@toggle-fn p s))))))

      [panel state-atom
       (fn [f] (reset! toggle-fn f))])))

(defn update! [state-atom current-step pat-snapshot]
  (swap! state-atom assoc :current current-step :pat pat-snapshot))

(defn set-kit! [state-atom kit]
  (swap! state-atom assoc :kit kit))

(defn make-scrollable [panel]
  (let [scroll (JScrollPane. panel
                             JScrollPane/VERTICAL_SCROLLBAR_NEVER
                             JScrollPane/HORIZONTAL_SCROLLBAR_NEVER)]
    (.setBackground scroll theme/bg)
    (.setBackground (.getViewport scroll) theme/bg)
    scroll))