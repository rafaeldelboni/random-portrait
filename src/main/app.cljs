(ns main.app
  (:require ["axios$default" :as axios]
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
   :img-src (gobj/getValueByKeys obj "links" "full")
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

(defn set-current [set-state current upcoming-list]
  (when (> (count upcoming-list) 0)
    (set-state update-in [:past :list] #(if %2 (into (take 10 %1) [%2]) %1) current)
    (set-state assoc :current (first upcoming-list))
    (set-state update-in [:upcoming :list] rest)
    (pre-load-image (first upcoming-list))
    (pre-load-image (second upcoming-list))))

(defnc countdown
  "Countdown component"
  [{:keys [seconds]}]
  (let [[state set-state] (hooks/use-state {:counter 0
                                            :current nil
                                            :past {:list []}
                                            :upcoming {:error nil
                                                       :loading false
                                                       :list []}})
        {:keys [counter current upcoming]} state
        upcoming-loading (:loading upcoming)
        upcoming-list (:list upcoming)
        {:keys [img-url desc]} current]

    (hooks/use-effect
      [upcoming-list upcoming-loading]
      (when (and (< (count upcoming-list) 2)
                 (not upcoming-loading))
        #_(let [images (take 5 (repeatedly #(rand-int 1000)))]
            (set-state update-in [:upcoming :list] #(into %2 %1) images))
        (get-30-random-portraits set-state)))

    (hooks/use-effect
      [current upcoming-list]
      (when (nil? current)
        (set-current set-state current upcoming-list)))

    (hooks/use-effect
      [counter]
      (if-not (zero? counter)
        (js/setTimeout #(set-state update-in [:counter] dec) 1000)
        (do (set-state assoc :counter seconds)
            (set-current set-state current upcoming-list))))

    (d/div
     (d/div {:className "text-center"}
            (d/div
             {:className "progress" :style #js {:height "15px"}}
             (let [progress (- 110 (* (/ counter seconds) 100))]
               (d/div {:className "progress-bar progress-bar-animated"
                       :role "progressbar"
                       :aria-valuenow (str progress)
                       :aria-valuemin "0"
                       :aria-valuemax "100"
                       :style #js {:width (str progress "%")}})))
            (d/div {:className "pt-3"}
                   (if current
                     (d/img {:src img-url :alt desc :className "img-fluid"})
                     (d/div
                      {:className "spinner-grow" :role "status"}
                      (d/span {:className "visually-hidden"} "Loading...")))))

     (d/pre
      {:className "pt-4"}
      (d/code
       (with-out-str (pprint/pprint state)))))))

;; app
(defnc app []
  (let [[_state _set-state] (hooks/use-state {})]
    (d/div
     {:className "container"}
     (d/header
      {:className "d-flex flex-wrap align-items-center justify-content-center justify-content-md-between py-3 mb-4 border-bottom"}
      (d/div
       {:className "d-flex align-items-center mb-2 mb-lg-0 text-dark text-decoration-none"}
       (d/span {:className "px-2 fs-4 text-dark"} "Random Portrait"))
      (d/div
       {:className "col-md-3 text-end"}
       (d/button {:className "btn btn-outline-dark me-2" :disabled true} "Config")))
     ($ countdown {:seconds 10}))))

;; start your app with your React renderer
(defn ^:export init []
  (doto (rdom/createRoot (js/document.getElementById "app"))
    (.render ($ app))))
