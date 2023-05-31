(ns main.containers
  (:require ["axios$default" :as axios]
            ["bootstrap"]
            [goog.object :as gobj]
            [helix.core :refer [$]]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [main.adapters :as adapters]
            [main.components :as components]
            [main.lib :refer [defnc]]))

; mutations

(defn pre-load-image! [{:keys [img-url]}]
  (-> (js/Promise. #(-> (js/Image.)
                        (.-src)
                        (set! img-url)))
      (.then #(println :ok %))
      (.catch #(println :error %))))

(defn get-30-random-portraits! [set-state]
  (set-state assoc :upcoming {:loading true :error nil})
  (-> (.get axios
            (str "https://corsproxy.io/?" ; sorry, just a POC
                 (js/encodeURIComponent "https://unsplash.com/napi/photos/random?query=portrait&count=30&")))
      (.then (fn [res]
               (let [data (gobj/get res "data")
                     images (map adapters/wire->model data)]
                 (set-state assoc-in [:upcoming :loading] false)
                 (set-state update-in [:upcoming :list] #(into %2 %1) images))))
      (.catch (fn [err]
                (set-state assoc :upcoming {:loading false :error err})))))

(defn tick-counter!
  [set-state]
  (set-state assoc :timeout-id (js/setTimeout #(set-state update-in [:counter] dec) 1000)))

(defn clear-tick-counter!
  [set-state timeout-id]
  (set-state assoc :timeout-id (js/clearTimeout timeout-id)))

(defn set-current-image!
  [set-state {:keys [upcoming current timeout-id]}]
  (let [upcoming-list (:list upcoming)]
    (when (> (count upcoming-list) 0)
      (clear-tick-counter! set-state timeout-id)
      (set-state update-in [:past :list] #(if %2 (into (take 10 %1) [%2]) %1) current)
      (set-state assoc :current (first upcoming-list))
      (set-state update-in [:upcoming :list] rest)
      (pre-load-image! (first upcoming-list))
      (pre-load-image! (second upcoming-list)))))

(defn update-total-seconds!
  [set-state {:keys [timeout-id seconds]}]
  (clear-tick-counter! set-state timeout-id)
  (set-state assoc :counter seconds))

(defn set-unset-pause!
  [set-state timeout-id pause]
  (if pause
    (clear-tick-counter! set-state timeout-id)
    (tick-counter! set-state)))

(defn reset-counter!
  [set-state seconds]
  (set-state assoc :counter seconds))

;; containers

(defnc photo-switcher
  "Countdown component"
  [{:keys [seconds pause debug] :as upper-state}]
  (let [[state set-state] (hooks/use-state {:counter 0
                                            :timeout-id nil
                                            :current nil
                                            :past {:list []}
                                            :upcoming {:error nil
                                                       :loading false
                                                       :list []}})
        {:keys [counter current upcoming]} state
        upcoming-loading (:loading upcoming)
        upcoming-list (:list upcoming)]

    (hooks/use-effect
      [upcoming-list upcoming-loading]
      (when (and (< (count upcoming-list) 2)
                 (not upcoming-loading))
        (get-30-random-portraits! set-state)))

    (hooks/use-effect
      [current upcoming-list]
      (when (nil? current)
        (set-current-image! set-state state)))

    (hooks/use-effect
      [pause]
      (set-unset-pause! set-state (:timeout-id state) pause))

    (hooks/use-effect
      [seconds]
      (update-total-seconds! set-state state))

    (hooks/use-effect
      [counter]
      (when-not pause
        (cond
          ; tick on positive
          (> counter 0) (tick-counter! set-state)
          ; zero
          :else (do (reset-counter! set-state seconds)
                    (set-current-image! set-state state)))))

    (d/div
     (d/div
      {:className "text-center"}
      ($ components/progress-bar
         {:counter counter :seconds seconds})
      ($ components/image-and-author
         {& (select-keys current [:author-src :author-name
                                  :current :desc :img-url :img-src])}))
     ($ components/debug-panel
        {:debug debug :state (merge state {:upper-state upper-state})}))))
