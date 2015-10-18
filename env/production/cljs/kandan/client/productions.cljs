(ns kandan.client.productions
  (:require [kandan.client.core :as kandan-core]
            [kandan.client.utils :as utils]))

(js/console.log "Loading Kandan via production..." (pr-str utils/initial-query-map))

(defn -main []
  (kandan-core/-main (js/document.getElementById "kandan-app") kandan-core/app-state))

(-main)
