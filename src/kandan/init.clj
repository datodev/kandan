(ns kandan.init
  (:require [kandan.datomic.core]
            [kandan.datomic.migrations]
            [kandan.datomic.schema]
            [kandan.nrepl]
            [kandan.server])
  (:import [java.util Date]))

(defn init []
  (kandan.nrepl/init)
  (kandan.datomic.core/init)
  (kandan.datomic.schema/init)
  (kandan.datomic.migrations/init)
  (kandan.server/init))

(defn -main []
  (println "Initializing dato at" (Date.))
  (init)
  (println "Finished initializing dato at" (Date.)))
