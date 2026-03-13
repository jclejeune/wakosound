(ns soundboard-clojure.sampler.keymap
  (:import [java.awt.event KeyEvent]))

;; ──────────────────────────────────────────
;; Mapping touches numpad → index de pad
;; Disposition MPC (7 8 9 / 4 5 6 / 1 2 3)
;; ──────────────────────────────────────────

(def numpad-keys
  [KeyEvent/VK_NUMPAD7 KeyEvent/VK_NUMPAD8 KeyEvent/VK_NUMPAD9
   KeyEvent/VK_NUMPAD4 KeyEvent/VK_NUMPAD5 KeyEvent/VK_NUMPAD6
   KeyEvent/VK_NUMPAD1 KeyEvent/VK_NUMPAD2 KeyEvent/VK_NUMPAD3])

(defn key->pad-idx
  "Retourne l'index du pad pour un KeyEvent donné, ou nil."
  [key-code]
  (first (keep-indexed
          (fn [idx k] (when (= k key-code) idx))
          numpad-keys)))