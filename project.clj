(defproject soundboard-clojure "0.1.0-SNAPSHOT"
  :description "WakoSound - Sampler MPC + Séquenceur FL"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/core.async "1.6.681"]
                 [javazoom/jlayer "1.0.1"]]
  :main soundboard-clojure.core
  :source-paths ["src"]
  :resource-paths ["resources"]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})