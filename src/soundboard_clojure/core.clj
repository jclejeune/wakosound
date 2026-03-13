(ns soundboard-clojure.core
  (:require [soundboard-clojure.ui.theme       :as theme]
            [soundboard-clojure.ui.main-window :as window])
  (:import [javax.swing SwingUtilities UIManager])
  (:gen-class))

(defn -main [& _args]
  (SwingUtilities/invokeLater
   #(do (theme/apply-global-theme!)
        (window/create!))))