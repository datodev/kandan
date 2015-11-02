(ns kandan.client.debug
  (:require [datascript :as d]
            [dato.lib.core :as dato]
            [dato.db.utils :as dsu]
            [kandan.client.db :as kdb]))

(def watched-expressions
  [{:title "Local session"
    :fn (fn [db] (let [local-session-ptrs (dsu/qes-by db :local/current-session)
                      sessions           (map (comp dsu/t :local/current-session) local-session-ptrs)
                      users              (map dsu/t (dsu/qes-by db :user/email))]
                  {:dato-session       (when-let [s (dato/local-session db)]
                                         (dsu/t s))
                   :local-session-ptrs (mapv dsu/t local-session-ptrs)
                   :users              users
                   :sessions           sessions}))}
   {:title "Kandan Meta"
    :fn (fn [db] {:finished-initial-loading? (kdb/finished-initial-loading? db)
                 :session-meta              (kdb/session-meta db)})}
   {:title "Channel ids:"
    :fn    (fn [db]
             {:channel-ids (mapv :db/id (dsu/qes-by db :channel/title))})}
   {:title "Dato session"
    :fn (fn [db] (let [local-session-ptrs (dsu/qes-by db :local/current-session)
                      sessions           (map (comp dsu/t :local/current-session) local-session-ptrs)]
                  {:dato-session (when-let [s (dato/local-session db)]
                                   (dsu/t s))}))}])
