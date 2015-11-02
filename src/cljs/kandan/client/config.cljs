(ns kandan.client.config)

(defn config []
  (aget js/window "Dato" "config"))

(defn dato-port []
  (aget (config) "dato-port"))

(def navigator-desc
  (let [navigator (.-navigator js/window)]
    {:appCodeName         (aget navigator "appCodeName")
     :appName             (aget navigator "appName")
     :appVersion          (aget navigator "appVersion")
     :cookieEnabled       (aget navigator "cookieEnabled")
     :hardwareConcurrency (aget navigator "hardwareConcurrency")
     :language            (aget navigator "language")
     :languages           (js->clj (aget navigator "languages"))
     :maxTouchPoints      (aget navigator "maxTouchPoints")
     :onLine              (aget navigator "onLine")
     :platform            (aget navigator "platform")
     :product             (aget navigator "product")
     :productSub          (aget navigator "productSub")
     :userAgent           (aget navigator "userAgent")
     :vendor              (aget navigator "vendor")
     :vendorSub           (aget navigator "vendorSub")
     :client-git-sha      "8b986b8"}))
