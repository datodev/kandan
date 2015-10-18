(ns kandan.config
  (:require [environ.core :as env]))

(defn datomic-uri []
  (or (env/env :datomic-uri)
      "datomic:free://localhost:4334/kandan_dev"))

(defn dev? []
  (env/env :is-dev))

(defn nrepl-port []
  (when-let [port (env/env :nrepl-port)]
    (Integer/parseInt port)))

(defn dato-port []
  (if-let [port (env/env :dato-port)]
    (Integer/parseInt port)
    8080))

(defn server-port []
  (if-let [port (env/env :server-port)]
    (Integer/parseInt port)
    10555))
