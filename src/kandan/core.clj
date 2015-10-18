(ns kandan.core
  (:require [kandan.server :as server])
  (:gen-class))

(defn -main
  [& port]
  (server/run port))
