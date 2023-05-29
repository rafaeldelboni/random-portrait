(ns main.app
  (:require
   ["react-dom/client" :as rdom]
   [helix.core :refer [$]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [main.lib :refer [defnc]]))

; https://unsplash.com/napi/photos/random?query=portrait

(defnc countdown
  "Countdown component"
  [{:keys [seconds]}]
  (let [[counter set-counter] (hooks/use-state seconds)]

    (hooks/use-effect
      [counter]
      (if-not (zero? counter)
        (js/setTimeout #(set-counter dec) 1000)
        (set-counter 10)))

    (d/div
     (str counter "s"))))

;; app
(defnc app []
  (let [[_state _set-state] (hooks/use-state {})]
    (d/div
     (d/h1 "random-portrait")
     (d/h2 "cc.delboni/random-portrait")
     (d/p "FIXME: generated by the rafaeldelboni/helix-scratch template.")
     ($ countdown {:seconds 10}))))

;; start your app with your React renderer
(defn ^:export init []
  (doto (rdom/createRoot (js/document.getElementById "app"))
    (.render ($ app))))
