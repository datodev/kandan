(ns kandan.client.components.utils
  (:require [clojure.string :as string]
            [datascript :as d]
            [dato.lib.core :as dato]
            [dato.lib.db :as db]
            [dato.db.utils :as dsu]
            [kandan.client.utils :as utils]))

(defn kill! [event]
  (doto event
    (.preventDefault)
    (.stopPropagation)))

(defn delay-focus!
  ([root selector]
   (delay-focus! root selector false))
  ([root selector select?]
   (js/setTimeout #(let [input (utils/sel1 root selector)]
                     (.focus input)
                     (when select?
                       (.select input))) 20)))

(defn esc? [event]
  (= (.. event -which) 27))

(defn enter? [event]
  (= (.. event -which) 13))

(defn shift? [event]
  (.-shiftKey event))

(def mime->file-type
  {"application/pdf" :file.type/pdf
   "image/png"       :file.type/image
   "image/jpg"       :file.type/image
   "image/jpeg"      :file.type/image})



