(ns kandan.models.user
  (:require [clojurewerkz.scrypt.core :as sc]
            [datomic.api :as d]
            [dato.db.utils :as dsu]))

(def cpu-cost
  16384)

(def ram-cost
  8)

(def parallelism-cost
  1)

(defn encrypt
  ([s]
   (encrypt s {}))
  ([s encrypt-options]
   (let [cpu-cost      (or (:cpu encrypt-options) cpu-cost)
         ram-cost      (or (:ram encrypt-options) ram-cost)
         parallel-cost (or (:parallel encrypt-options) parallelism-cost)])
   (sc/encrypt s cpu-cost ram-cost parallelism-cost)))

(defn set-password! [conn user-eid unencrypted-password]
  (let [encrypted-password (encrypt unencrypted-password)]
    (d/transact conn [[:db/add user-eid :user/password encrypted-password]])))

(defn by-email-and-unencrypted-password [db email unencrypted-password]
  (when-let [user (dsu/qe-by db :user/email email)]
    (when (and (:user/password user)
               unencrypted-password
               (sc/verify unencrypted-password (:user/password user)))
      user)))
