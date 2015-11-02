(ns kandan.client.controllers.controls
  (:require [cljs.core.async :as async :refer [>! <! alts! chan sliding-buffer put! close!]]
            [cljs.reader :as reader]
            [clojure.string :as string]
            [datascript :as d]
            [dato.lib.core :as dato]
            [dato.lib.controller :as dcon]
            [dato.db.utils :as dsu]
            [kandan.client.db :as kdb]
            [pushy.core :as pushy]))

(defmethod dcon/transition :server/initial-pull-succeeded
  [db payload]
  (let [{:keys [data]}    payload
        data              (:results data)
        me                (assoc-in (dissoc data :org/_users) [:user/me?] true)
        orgs              (get data :org/_users)
        other-users       (get-in data [:org/_users 0 :org/users])
        inspected-channel (get-in data [:org/_users 0 :org/channels 0 :db/id])
        new-session-id    (d/tempid :db.part/user)
        new-session       {:db/id             new-session-id
                           :session/user      {:db/id (:db/id me)}
                           :session/inspected inspected-channel}
        local-session     {:db/id                 (dsu/q1-by db :local/current-session)
                           :local/current-session new-session-id}
        loading-finished  {:db/id                        (or (dsu/q1-by db :kandan/meta)
                                                             (d/tempid :db.part/user))
                           :kandan/meta                  true
                           :kandan.meta/initial-loading? false}
        final-tx          (concat
                           [me new-session local-session loading-finished]
                           orgs
                           other-users)]
    final-tx))

(defmethod dcon/effect! :server/initial-pull-succeeded
  [{:keys [router]} old-db new-db exhibit]
  ;;(routes/start! router)
  )
