(ns soundboard-clojure.ui.main-window
  (:require [soundboard-clojure.ui.theme            :as theme]
            [soundboard-clojure.ui.pad-grid         :as pad-grid]
            [soundboard-clojure.ui.step-grid        :as step-grid]
            [soundboard-clojure.ui.transport-bar    :as transport]
            [soundboard-clojure.model.kit-manager   :as km]
            [soundboard-clojure.sampler.sampler     :as sampler]
            [soundboard-clojure.sampler.keymap      :as keymap]
            [soundboard-clojure.sequencer.pattern   :as pattern]
            [soundboard-clojure.sequencer.transport :as trans])
  (:import [java.awt BorderLayout FlowLayout Dimension]
           [javax.swing
            JFrame JPanel JLabel JTabbedPane JSeparator
            SwingConstants SwingUtilities]
           [java.awt.event ActionListener KeyAdapter]))

(defonce app-state
  (atom {:kits-state  nil
         :seq-pattern (atom (pattern/initial-state))
         :seq-stop    nil
         :play-mode   :one-shot}))

(defn- current-kit []
  (km/current-kit (:kits-state @app-state)))

(defn- kit-names []
  (vec (map :name (vals (:kits (:kits-state @app-state))))))

(defn create! []
  (swap! app-state assoc :kits-state (km/build-kits-state))

  (let [pad-buttons  (pad-grid/make-pad-buttons)
        [transport-panel transport-comps] (transport/make-panel)

        {:keys [play-btn clear-btn step-label
                bpm-spinner length-spinner
                one-shot-btn gate-btn]} transport-comps

        ;; ── Grille custom ──
        [grid-panel grid-state set-toggle!] (step-grid/make-panel (current-kit))

        kit-combo (theme/styled-combo (kit-names))
        kit-label (theme/status-label "")

        sampler-panel (doto (JPanel. (BorderLayout.))
                        (.setBackground theme/bg)
                        (.add (pad-grid/make-panel pad-buttons)
                              BorderLayout/CENTER))

        sequencer-panel (doto (JPanel. (BorderLayout.))
                          (.setBackground theme/bg)
                          (.add transport-panel BorderLayout/NORTH)
                          (.add (step-grid/make-scrollable grid-panel)
                                BorderLayout/CENTER))

        tabs (doto (JTabbedPane.)
               (.setBackground theme/bg)
               (.setForeground theme/fg)
               (.addTab "🥁 Sampler"    sampler-panel)
               (.addTab "🎛 Séquenceur" sequencer-panel))

        top-panel (doto (JPanel. (FlowLayout. FlowLayout/LEFT 6 4))
                    (.setBackground theme/bg)
                    (.add (theme/styled-label "Kit:"))
                    (.add kit-combo)
                    (.add kit-label))

        frame (doto (JFrame. "WakoSound")
                (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
                (.setLayout (BorderLayout.))
                (.setBackground theme/bg))]

    (.add frame top-panel BorderLayout/NORTH)
    (.add frame tabs BorderLayout/CENTER)

    (letfn [(refresh-kit-label! []
              (let [kit (current-kit)]
                (SwingUtilities/invokeLater
                 #(.setText kit-label
                            (format "  %s | %d/9 pads"
                                    (:name kit)
                                    (count (:pads kit)))))))

            ;; ── on-step! : non-bloquant, repaint() thread-safe ──
            (on-step! [step]
              (let [pat-snapshot @(:seq-pattern @app-state)]
                ;; Mettre à jour le state de la grille
                (step-grid/update! grid-state step pat-snapshot)
                ;; repaint() est thread-safe — Swing coalesce les repaints
                (.repaint grid-panel)
                ;; Label step sur l'EDT
                (SwingUtilities/invokeLater
                 #(transport/set-step-text! step-label step))))

            (toggle-play! []
              (if (trans/playing? app-state)
                (do (trans/stop! app-state)
                    (SwingUtilities/invokeLater
                     #(do (transport/set-playing-text! play-btn false)
                          (step-grid/update! grid-state -1
                                             @(:seq-pattern @app-state))
                          (.repaint grid-panel))))
                (do (trans/play! app-state (atom (current-kit)) on-step!)
                    (SwingUtilities/invokeLater
                     #(transport/set-playing-text! play-btn true)))))]

      ;; Toggle steps via mouse (câblé dans make-panel)
      (set-toggle!
       (fn [pad step]
         (swap! (:seq-pattern @app-state) pattern/toggle-step pad step)
         (step-grid/update! grid-state -1 @(:seq-pattern @app-state))
         (.repaint grid-panel)))

      ;; Pads souris
      (pad-grid/wire-actions! pad-buttons
                              (fn [idx] (sampler/play-pad! app-state idx)))

      ;; Pads numpad
      (.addKeyListener frame
                       (proxy [KeyAdapter] []
                         (keyPressed [e]
                           (when-let [idx (keymap/key->pad-idx (.getKeyCode e))]
                             (sampler/play-pad! app-state idx)
                             (pad-grid/flash! (nth pad-buttons idx))))))
      (.setFocusable frame true)

      ;; Play/Stop
      (.addActionListener play-btn
                          (reify ActionListener (actionPerformed [_ _] (toggle-play!))))

      ;; Clear
      (.addActionListener clear-btn
                          (reify ActionListener
                            (actionPerformed [_ _]
                              (swap! (:seq-pattern @app-state) pattern/clear-pattern)
                              (step-grid/update! grid-state -1 @(:seq-pattern @app-state))
                              (.repaint grid-panel))))

      ;; BPM
      (.addChangeListener bpm-spinner
                          (reify javax.swing.event.ChangeListener
                            (stateChanged [_ _]
                              (swap! (:seq-pattern @app-state)
                                     pattern/set-bpm (.getValue bpm-spinner)))))

      ;; Length
      (.addChangeListener length-spinner
                          (reify javax.swing.event.ChangeListener
                            (stateChanged [_ _]
                              (swap! (:seq-pattern @app-state)
                                     pattern/set-pattern-length (.getValue length-spinner)))))

      ;; Mode One-Shot / Gate
      (.addActionListener one-shot-btn
                          (reify ActionListener
                            (actionPerformed [_ _]
                              (trans/set-play-mode! app-state :one-shot))))
      (.addActionListener gate-btn
                          (reify ActionListener
                            (actionPerformed [_ _]
                              (trans/set-play-mode! app-state :gate))))

      ;; Kit selector
      (.addActionListener kit-combo
                          (reify ActionListener
                            (actionPerformed [_ _]
                              (when-let [name (.getSelectedItem kit-combo)]
                                (swap! app-state update :kits-state
                                       km/switch-kit-by-name name)
                                (step-grid/set-kit! grid-state (current-kit))
                                (.repaint grid-panel)
                                (pad-grid/refresh! pad-buttons (current-kit))
                                (refresh-kit-label!)))))

      ;; Stop séquenceur si retour onglet Sampler
      (.addChangeListener tabs
                          (reify javax.swing.event.ChangeListener
                            (stateChanged [_ _]
                              (when (= 0 (.getSelectedIndex tabs))
                                (when (trans/playing? app-state)
                                  (trans/stop! app-state)
                                  (SwingUtilities/invokeLater
                                   #(transport/set-playing-text! play-btn false)))))))

      (pad-grid/refresh! pad-buttons (current-kit))
      (refresh-kit-label!)
      (.setSize frame (Dimension. 620 520))
      (.setVisible frame true))))