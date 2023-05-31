(ns main.components
  (:require ["bootstrap"]
            [clojure.pprint :as pprint]
            [helix.core :refer [$]]
            [helix.dom :as d]
            [main.adapters :as adapters]
            [main.lib :refer [defnc]]))

(defnc progress-bar
  "Centralized Progress bar"
  [{:keys [counter seconds]}]
  (d/div
   {:className "progress" :style #js {:height "5px"}}
   (let [progress (adapters/values->percent counter seconds)]
     (d/div {:className "progress-bar progress-bar-animated bg-dark"
             :role "progressbar"
             :aria-valuenow (str progress)
             :aria-valuemin "0"
             :aria-valuemax "100"
             :style #js {:width (str progress "%")}}))))

(defnc image-and-author
  "Responsive image and author data"
  [{:keys [author-src author-name desc img-url img-src]}]
  (d/div
   {:className "pt-3"}
   (if img-url
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

(defnc debug-panel
  "Prints state debug data"
  [{:keys [debug state]}]
  (when debug
    (d/pre
     {:className "pt-4"}
     (d/div
      {:className "bg-light p-3"}
      (d/code
       (with-out-str (pprint/pprint state)))))))

(defnc config-dropdown
  "Config menu dropdown"
  [{:keys [seconds pause debug set-state]}]
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
        "Source code")))))))

(defnc header-navbar
  "Navbar"
  [{:keys [state set-state]}]
  (let [{:keys [seconds debug pause]} state]
    (d/header
     {:className "d-flex flex-wrap align-items-center justify-content-center justify-content-md-between py-3 mb-2"}
     (d/div
      {:className "d-flex align-items-center mb-2 mb-lg-0 text-dark text-decoration-none"}
      (d/span {:className "pt-2 px-2 fs-2 display-6 text-dark"} "Random Portrait"))
     (d/div
      {:className "col-md-3 text-end"}
      ($ config-dropdown {:seconds seconds
                          :debug debug
                          :pause pause
                          :set-state set-state})))))

