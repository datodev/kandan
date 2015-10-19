(ns ^:figwheel-always kandan.client.db
    (:require [datascript :as d]
              [dato.lib.db :as db]
              [dato.db.utils :as dsu]))

(def cs-schema
  {:local/current-session {:db/valueType :db.type/ref}
   :session/user          {:db/valueType :db.type/ref}
   :summon/session        {:db/valueType :db.type/ref}})

(defn my-initial-session [me]
  (let [db-id       (d/tempid :db.part/user)
        session-key (d/squuid)]
    [{:db/id        db-id
      :session/user (:db/id me)
      :session/key  session-key}
     {:local/current-session db-id}]))

(defn local-session [db]
  (d/entity db (dsu/val-by db :local/current-session)))

(defn me [db]
  (or ;;(dsu/qe-by db :user/me? true)
      ;; Placeholder until sessions are in place
      (dsu/qe-by db :user/email)))

(defn inspected-channel [db session]
  ;; Placeholder until sessions are in place
  (dsu/qe-by db :channel/title))
