(ns kandan.client.controllers.controls
  (:require [cljs.core.async :as async :refer [>! <! alts! chan sliding-buffer put! close!]]
            [cljs.reader :as reader]
            [clojure.string :as string]
            [datascript :as d]
            [dato.lib.core :as dato]
            [dato.lib.controller :as dcon]
            [dato.db.utils :as dsu]
            [pushy.core :as pushy]))

(defmethod dcon/transition :server/initial-pull-succeeded
  [db payload]
  (let [{:keys [data]}   payload
        data             (:results data)
        me               (assoc data :user/me? true)
        other-users      (get-in data [:org/_users :org/users])
        session-id       (d/tempid :db.part/user)
        new-session      {:db/id        session-id
                          :session/user {:db/id (:db/id me)}}
        local-session    {:local/current-session session-id}
        loading-finished {:db/id                      (dsu/q1-by db :mana.meta/initial-loading?)
                          :mana.meta/initial-loading? false}]
    (concat
     [me new-session local-session loading-finished]
     other-users)))

(defmethod dcon/effect! :server/initial-pull-succeeded
  [{:keys [router]} old-db new-db exhibit]
  (js/console.log "Now: " (dsu/qes-by new-db :channel/title))
  ;;(routes/start! router)
  )
