(ns main.adapters
  (:require ["bootstrap"]
            [goog.object :as gobj]))

(defn wire->model
  [obj]
  {:id (gobj/get obj "id")
   :desc (gobj/get obj "alt_description")
   :img-url (str (gobj/getValueByKeys obj "urls" "regular") "&h=1000")
   :img-src (gobj/getValueByKeys obj "links" "html")
   :author-name (gobj/getValueByKeys obj "user" "name")
   :author-src (gobj/getValueByKeys obj "user" "links" "html")})

(defn values->percent
  [current-value total]
  (- 100 (* (/ current-value total) 100)))
