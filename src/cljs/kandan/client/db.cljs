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
  (dsu/qe-by db :user/me? true))

(defn inspected-channel [db session]
  (let [inspected-channel (:session/inspected session)]
    (d/entity db inspected-channel)))

(defn session-meta [db]
  (dsu/qe-by db :kandan/meta true))

(defn finished-initial-loading? [db]
  (not (:kandan.meta/initial-loading? (session-meta db))))
