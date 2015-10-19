(ns kandan.client.config)

(defn config []
  (aget js/window "Dato" "config"))

(defn dato-port []
  (aget (config) "dato-port"))
