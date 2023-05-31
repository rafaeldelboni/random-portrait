(ns main.app
  (:require ["bootstrap"]
            ["react-dom/client" :as rdom]
            [helix.core :refer [$]]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [main.components :as components]
            [main.containers :as containers]
            [main.lib :refer [defnc]]))

;; app

(defnc app []
  (let [[state set-state] (hooks/use-state {:seconds 60 :pause false :debug false})
        handle-key-press (hooks/use-callback
                           :auto-deps
                           (fn [e]
                             (when (= (.-keyCode e) 32) ; space key
                               (.preventDefault e)
                               (set-state assoc :pause (not (:pause state))))))
        {:keys [seconds debug pause]} state]

    (hooks/use-effect
      [handle-key-press]
      (.addEventListener js/document "keydown" handle-key-press)
      (fn [] (.removeEventListener js/document "keydown" handle-key-press)))

    (d/div
     {:className "container"}
     ($ components/header-navbar {:state state :set-state set-state})
     ($ containers/photo-switcher {:seconds seconds
                                   :pause pause
                                   :debug debug}))))

;; start your app with your React renderer
(defn ^:export init []
  (doto (rdom/createRoot (js/document.getElementById "app"))
    (.render ($ app))))
