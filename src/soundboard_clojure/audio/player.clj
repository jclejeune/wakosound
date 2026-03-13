(ns soundboard-clojure.audio.player
  (:require [clojure.string :as str])
  (:import [javax.sound.sampled
            AudioSystem AudioFormat AudioInputStream Clip DataLine$Info]
           [java.io File ByteArrayInputStream ByteArrayOutputStream]
           [javazoom.jl.player Player]))

;; ──────────────────────────────────────────────────────────────
;; Cache de données audio brutes (byte array + format)
;;
;; On charge le fichier UNE SEULE FOIS en RAM.
;; À chaque lecture, on crée un Clip depuis les bytes en mémoire
;; → pas d'accès disque, pas de saturation PipeWire
;; → polyphonie réelle (plusieurs Clips indépendants par sample)
;; → Gate fonctionne : chaque Clip est une instance distincte
;; ──────────────────────────────────────────────────────────────

(defonce ^:private audio-cache (atom {}))  ; file-path → {:bytes [] :format fmt}

(def ^:private audio-pool
  ;; Queue bornee a 8 slots + DiscardPolicy = DROP si pleine
  ;; Evite l'accumulation de sons en retard a haut BPM
  (java.util.concurrent.ThreadPoolExecutor.
   8 8
   0 java.util.concurrent.TimeUnit/MILLISECONDS
   (java.util.concurrent.ArrayBlockingQueue. 8)
   (java.util.concurrent.ThreadPoolExecutor$DiscardPolicy.)))

(defn- to-stereo [^AudioInputStream ais]
  (let [fmt (.getFormat ais)]
    (if (= 1 (.getChannels fmt))
      (let [stereo (AudioFormat. (.getEncoding fmt)
                                 (.getSampleRate fmt)
                                 (.getSampleSizeInBits fmt)
                                 2
                                 (* 2 (.getFrameSize fmt))
                                 (.getFrameRate fmt)
                                 (.isBigEndian fmt))]
        (AudioSystem/getAudioInputStream stereo ais))
      ais)))

(defn- load-audio-data! [file-path]
  (try
    (let [raw  (AudioSystem/getAudioInputStream (File. file-path))
          ais  (to-stereo raw)
          fmt  (.getFormat ais)
          buf  (ByteArrayOutputStream.)]
      (let [chunk (byte-array 4096)]
        (loop []
          (let [n (.read ais chunk)]
            (when (> n 0)
              (.write buf chunk 0 n)
              (recur)))))
      (.close ais)
      (.close raw)
      {:bytes  (.toByteArray buf)
       :format fmt})
    (catch Exception e
      (println "WARN: impossible de charger" file-path "-" (.getMessage e))
      nil)))

(defn- get-audio-data! [file-path]
  (or (get @audio-cache file-path)
      (when-let [data (load-audio-data! file-path)]
        (swap! audio-cache assoc file-path data)
        data)))

(defn- make-clip
  "Crée un nouveau Clip depuis les données en mémoire.
   Léger — pas d'I/O disque."
  ^Clip [{:keys [^bytes bytes ^AudioFormat format]}]
  (try
    (let [bais (ByteArrayInputStream. bytes)
          ais  (AudioInputStream. bais format
                                  (/ (alength bytes)
                                     (.getFrameSize format)))
          info (DataLine$Info. Clip format)
          clip (AudioSystem/getLine info)]
      (.open ^Clip clip ais)
      clip)
    (catch Exception e
      (println "Erreur make-clip" (.getMessage e))
      nil)))

;; ──────────────────────────────────────────────────────────────
;; API publique
;; ──────────────────────────────────────────────────────────────

(defn play-wav!
  "Joue un WAV. Retourne un atom qui contiendra le Clip
   dès qu'il est démarré (pour stop! en mode Gate)."
  [file-path]
  (let [clip-ref (atom nil)]
    (.submit audio-pool
             ^Callable
             (fn []
               (when-let [data (get-audio-data! file-path)]
                 (when-let [^Clip clip (make-clip data)]
                   (reset! clip-ref clip)
                   (.start clip)
                   ;; Auto-close quand terminé
                   (.addLineListener clip
                                     (reify javax.sound.sampled.LineListener
                                       (update [_ event]
                                         (when (= (.getType event)
                                                  javax.sound.sampled.LineEvent$Type/STOP)
                                           (.close clip)))))))))
    clip-ref))

(defn- play-mp3! [file-path]
  (.submit audio-pool
           ^Callable
           (fn []
             (try
               (with-open [fis (java.io.FileInputStream. file-path)]
                 (.play (Player. fis)))
               (catch Exception e
                 (println "Erreur MP3:" (.getMessage e)))))))

(defn play!
  "Joue un fichier. Retourne un atom clip-ref (WAV) ou nil (MP3)."
  [file-path]
  (when (not-empty file-path)
    (if (str/ends-with? (str/lower-case file-path) ".mp3")
      (do (play-mp3! file-path) nil)
      (play-wav! file-path))))

(defn stop-clip!
  "Stoppe un clip-ref (mode Gate). Non-bloquant."
  [clip-ref]
  (when (instance? clojure.lang.Atom clip-ref)
    (.submit audio-pool
             ^Callable
             (fn []
               (let [deadline (+ (System/currentTimeMillis) 150)]
                 (loop []
                   (let [clip @clip-ref]
                     (cond
                       (instance? Clip clip)
                       (try (.stop ^Clip clip) (catch Exception _))
                       (< (System/currentTimeMillis) deadline)
                       (do (Thread/sleep 5) (recur))))))))))

(defn preload!
  "Précharge les données audio d'un kit en RAM au démarrage."
  [kit]
  (doseq [pad (vals (:pads kit))
          :when (:file-path pad)]
    (get-audio-data! (:file-path pad))))

(defn ^:export clear-cache! []
  (reset! audio-cache {}))