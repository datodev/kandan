(ns ^:figwheel-always kandan.client.dev
  (:require [dato.debug.components.core :as dato-debug]
            [devtools.core :as devtools]
            [figwheel.client :as figwheel :include-macros true]
            [kandan.client.core :as kandan-core]
            [kandan.client.debug :as todo-debug]
            [kandan.client.utils :as utils]
            [om.core :as om]
            [om-i.core :as om-i]
            [om-i.hacks :as om-i-hacks]
            [weasel.repl :as weasel]))

(defn dev-connect! []
  #_(figwheel/watch-and-reload
   :websocket-url "ws://localhost:3449/figwheel-ws"
   :jsload-callback (fn [] (js/console.log "Reloading from figwheel..")))
  (when (:weasel? utils/initial-query-map)
    (weasel/connect "ws://localhost:9001" :verbose true :print #{:repl :console}))
  true)

(defonce setup-misc
  (do
    (dev-connect!)
    (om-i-hacks/insert-styles)
    (om-i/setup-component-stats!)
    ;; Enable https://github.com/binaryage/cljs-devtools
    (js/console.log "Installing devtools...")
    (devtools/install!)
    (js/console.log "Loading Kandan via dev..." utils/initial-query-map)))

(defn -main []
  (let [root-node     (js/document.getElementById "kandan-app")
        app-state     kandan-core/app-state
        dato          (:dato @app-state)
        app-root      (kandan-core/-main root-node
                                            app-state
                                            {:om-instrument (fn [f cursor m]
                                                              (let [com (satisfies? om/IDisplayName (f))])
                                                              (om/build* f cursor
                                                                         (if (:descriptor m)
                                                                           m
                                                                           (assoc m :descriptor
                                                                                  dato-debug/watch-state-methods
                                                                                  ;; TODO: Figure out how to compose these
                                                                                  ;; om-i/instrumentation-methods
                                                                                  ))))})]
    (om/root dato-debug/devtools {:dato dato}
             {:target (utils/sel1 root-node :.debugger-container)
              :shared {:dato     dato
                       :app-root app-root}
              :opts   {:expressions todo-debug/watched-expressions}})))

(-main)
