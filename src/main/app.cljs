(ns main.app
  (:require ["axios$default" :as axios]
            ["bootstrap"]
            ["react-dom/client" :as rdom]
            [clojure.pprint :as pprint]
            [goog.object :as gobj]
            [helix.core :refer [$]]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [main.lib :refer [defnc]]))

(defn pre-load-image [{:keys [img-url]}]
  (-> (js/Promise. #(-> (js/Image.)
                        (.-src)
                        (set! img-url)))
      (.then #(println :ok %))
      (.catch #(println :error %))))

(defn wire->model
  [obj]
  {:id (gobj/get obj "id")
   :desc (gobj/get obj "alt_description")
   :img-url (str (gobj/getValueByKeys obj "urls" "regular") "&h=1000")
   :img-src (gobj/getValueByKeys obj "links" "html")
   :author-name (gobj/getValueByKeys obj "user" "name")
   :author-src (gobj/getValueByKeys obj "user" "links" "html")})

(defn get-30-random-portraits [set-state]
  (set-state assoc :upcoming {:loading true :error nil})
  (-> (.get axios
            (str "https://corsproxy.io/?" ; sorry, just a POC
                 (js/encodeURIComponent "https://unsplash.com/napi/photos/random?query=portrait&count=30&")))
      (.then (fn [res]
               (let [data (gobj/get res "data")
                     images (map wire->model data)]
                 (set-state assoc-in [:upcoming :loading] false)
                 (set-state update-in [:upcoming :list] #(into %2 %1) images))))
      (.catch (fn [err]
                (set-state assoc :upcoming {:loading false :error err})))))

(defn set-current [set-state {:keys [upcoming current timeout-id]}]
  (let [upcoming-list (:list upcoming)]
    (when (> (count upcoming-list) 0)
      (set-state assoc :timeout-id (js/clearTimeout timeout-id))
      (set-state update-in [:past :list] #(if %2 (into (take 10 %1) [%2]) %1) current)
      (set-state assoc :current (first upcoming-list))
      (set-state update-in [:upcoming :list] rest)
      (pre-load-image (first upcoming-list))
      (pre-load-image (second upcoming-list)))))

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
        upcoming-list (:list upcoming)
        {:keys [img-url desc author-name author-src img-src]} current]

    (hooks/use-effect
      [upcoming-list upcoming-loading]
      (when (and (< (count upcoming-list) 2)
                 (not upcoming-loading))
        (get-30-random-portraits set-state)))

    (hooks/use-effect
      [current upcoming-list]
      (when (nil? current)
        (set-current set-state state)))

    (hooks/use-effect
      [pause]
      (if pause
        (set-state assoc :timeout-id (js/clearTimeout (:timeout-id state)))
        (set-state assoc :timeout-id (js/setTimeout #(set-state update-in [:counter] dec) 1000))))

    (hooks/use-effect
      [seconds]
      (do (set-state assoc :timeout-id (js/clearTimeout (:timeout-id state)))
          (set-state assoc :counter seconds)))

    (hooks/use-effect
      [counter]
      (when-not pause
        (cond
          ; tick on positive
          (> counter 0) (set-state assoc :timeout-id (js/setTimeout #(set-state update-in [:counter] dec) 1000))
          ; zero
          :else (do (set-state assoc :counter seconds)
                    (set-current set-state state)))))

    (d/div
     (d/div
      {:className "text-center"}
      (d/div
       {:className "progress" :style #js {:height "5px"}}
       (let [progress (- 100 (* (/ counter seconds) 100))]
         (d/div {:className "progress-bar progress-bar-animated bg-dark"
                 :role "progressbar"
                 :aria-valuenow (str progress)
                 :aria-valuemin "0"
                 :aria-valuemax "100"
                 :style #js {:width (str progress "%")}})))

      (d/div
       {:className "pt-3"}
       (if current
         (d/div
          (d/img {:src img-url :alt desc :className "img-fluid"})
          (d/div
           (d/p
            {:className "mt-2 text-end"}
            (d/small
             "Photo by "
             (d/a
              {:className "link-dark"
               :href author-src}
              author-name)
             " on "
             (d/a
              {:className "link-dark"
               :href img-src}
              "Unsplash")))))
         (d/div
          {:className "spinner-grow" :role "status"}
          (d/span {:className "visually-hidden"} "Loading...")))))

     (when debug
       (d/pre
        {:className "pt-4"}
        (d/div
         {:className "bg-light p-3"}
         (d/code
          (with-out-str (pprint/pprint (merge state {:upper-state upper-state}))))))))))

;; app
(defnc app []
  (let [[state set-state] (hooks/use-state {:seconds 60
                                            :pause false
                                            :debug false})
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
     (d/header
      {:className "d-flex flex-wrap align-items-center justify-content-center justify-content-md-between py-3 mb-2"}
      (d/div
       {:className "d-flex align-items-center mb-2 mb-lg-0 text-dark text-decoration-none"}
       (d/span {:className "pt-2 px-2 fs-2 display-6 text-dark"} "Random Portrait"))
      (d/div
       {:className "col-md-3 text-end"}
       (d/div
        {:className "dropdown px-2 fs-2"}
        (d/button
         {:className "btn btn-outline-dark me-2 dropdown-toggle"
          :data-bs-toggle "dropdown"
          :aria-expanded "false"
          :data-bs-auto-close "outside"}
         "Config")
        (d/div
         {:className "dropdown-menu dropdown-menu-end p-4"}
         (d/div
          {:className "mb-3"}
          (d/label {:className "form-label"
                    :for "seconds-input"} "Seconds")
          (d/input {:className "form-control"
                    :type "number"
                    :id "seconds-input"
                    :value seconds
                    :on-change #(set-state assoc :seconds (js/Number (.. % -target -value)))}))
         (d/div
          {:className "mb-3"}
          (d/div
           {:className "form-check form-switch"}
           (d/label {:className "form-check-label" :for "pause-switch"}
                    (d/span {:className "pe-2"} "Pause")
                    (d/kbd "Space"))
           (d/input {:className "form-check-input"
                     :type "checkbox"
                     :id "pause-switch"
                     :role "switch"
                     :checked pause
                     :on-change #(set-state assoc :pause (js/Boolean (.. % -target -checked)))})))
         (d/div
          {:className "mb-3"}
          (d/div
           {:className "form-check form-switch"}
           (d/label {:className "form-check-label" :for "debug-switch"} "Debug")
           (d/input {:className "form-check-input"
                     :type "checkbox"
                     :id "debug-switch"
                     :role "switch"
                     :checked debug
                     :on-change #(set-state assoc :debug (js/Boolean (.. % -target -checked)))})))

         (d/div
          (d/p
           {:className "mb-0 text-end"}
           (d/small
            (d/a
             {:className "link-dark"
              :href "https://github.com/rafaeldelboni/random-portrait"}
             "Source code"))))))))

     ($ photo-switcher {:seconds seconds
                        :pause pause
                        :debug debug}))))

;; start your app with your React renderer
(defn ^:export init []
  (doto (rdom/createRoot (js/document.getElementById "app"))
    (.render ($ app))))
