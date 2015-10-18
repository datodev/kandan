(ns kandan.client.config)

(defn config []
  (aget js/window "Kandan" "config"))

(defn dato-port []
  (aget (config) "dato-port"))
