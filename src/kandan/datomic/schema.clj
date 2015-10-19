(ns kandan.datomic.schema
  (:require [kandan.datomic.core :as db-conn]
            [dato.db.utils :as dsu]
            [datomic.api :refer [db q] :as d]))

(defn attribute [ident type & {:as opts}]
  (merge {:db/id                 (d/tempid :db.part/db)
          :db/ident              ident
          :db/valueType          type
          :db.install/_attribute :db.part/db}
         {:db/cardinality :db.cardinality/one}
         opts))

(defn function [ident fn & {:as opts}]
  (merge {:db/id    (d/tempid :db.part/user)
          :db/ident ident
          :db/fn    fn}
         opts))

(defn enum [ident]
  {:db/id     (d/tempid :db.part/user)
   :db/ident  ident
   :dato/guid (d/squuid)})

(def schema-1
   ;; Universal attributes
  [(attribute :migration/version :db.type/long)
   (attribute :dato/type :db.type/ref)
   (attribute :dato/guid :db.type/uuid :db/unique :db.unique/identity)
   (attribute :tx/guid :db.type/uuid :db/unique :db.unique/identity)
   (attribute :tx/session-id :db.type/string)])

(def schema-2
  (vec
   (concat
    ;; Kandan Types
    [(enum :kandan.type/org)
     (enum :kandan.type/user)
     (enum :kandan.type/channel)
     (enum :kandan.type/msg)
     (enum :kandan.type/file)
     (enum :kandan.type/notification-setting)]

    ;; Orgs
    [(attribute :org/name       :db.type/string :db/unique :db.unique/value)
     (attribute :org/icon       :db.type/uri)
     (attribute :org/users      :db.type/ref
                :db/cardinality :db.cardinality/many)
     (attribute :org/admins     :db.type/ref
                :db/cardinality :db.cardinality/many)
     (attribute :org/channels   :db.type/ref
                :db/cardinality :db.cardinality/many)]

    ;; Users
    [(attribute :user/disabled?   :db.type/boolean)
     (attribute :user/email       :db.type/string :db/unique :db.unique/identity)
     (attribute :user/nick        :db.type/string)
     (attribute :user/given-name  :db.type/string)
     (attribute :user/family-name :db.type/string)
     (attribute :user/password    :db.type/string)
     (attribute :user/avatar      :db.type/uri)
     (attribute :user/notification-settings :db.type/ref
                :db/cardinality :db.cardinality/many
                :db/isComponent true)]
    
    ;; Channels
    [(attribute :channel/title   :db.type/string)
     (attribute :channel/topic   :db.type/string)
     (attribute :channel/created-at   :db.type/instant)
     (attribute :channel/admins  :db.type/ref
                :db/cardinality  :db.cardinality/many)
     (attribute :channel/members :db.type/ref
                :db/cardinality  :db.cardinality/many)
     (attribute :channel/msgs    :db.type/ref
                :db/cardinality  :db.cardinality/many
                :db/isComponent  true)
     (attribute :channel/files   :db.type/ref
                :db/cardinality  :db.cardinality/many
                :db/isComponent  true)
     (attribute :channel/notification-settings :db.type/ref
                :db/cardinality :db.cardinality/many
                :db/isComponent true)]

    [(enum :notification.level/all)
     (enum :notification.level/me)
     (enum :notification.level/none)]

    ;; Notification setting
    [(attribute :notification/general           :db.type/ref)
     (attribute :notification/everyone-and-here :db.type/ref)
     (attribute :notification/muted?            :db.type/boolean)]

    ;; Messages
    [(attribute :msg/body       :db.type/string)
     (attribute :msg/at         :db.type/instant)
     (attribute :msg/user       :db.type/ref)
     (attribute :msg/pinned?    :db.type/boolean)
     (attribute :msg/file       :db.type/ref
                :db/isComponent true)]

    ;; File types
    [(enum :file.type/pdf)
     (enum :file.type/image)
     (enum :file.type/link)
     (enum :file.type/text)
     (enum :file.type/office-word)
     (enum :file.type/office-excel)
     (enum :file.type/office-powerpoint)
     (enum :file.type/unknown)]

    ;; File
    [(attribute :file/name               :db.type/string)
     (attribute :file/comment            :db.type/string)
     (attribute :file/failed?            :db.type/boolean)
     (attribute :file/last-modified      :db.type/long)
     (attribute :file/last-modified-date :db.type/instant)
     (attribute :file/size               :db.type/long)
     (attribute :file/type               :db.type/ref)
     (attribute :file/mime               :db.type/string)
     (attribute :file/src                :db.type/uri)
     (attribute :file/progress           :db.type/long)])))


(defonce schema-ents
  (atom nil))

(defn enums []
  (->> @schema-ents
       (filter #(= :db.type/ref (:db/valueType %)))
       (map :db/ident )
       (set)))

(defn get-ident [a]
  (->> @schema-ents
       (filter #(= a (:db/id %)) )
       first 
       :db/ident))

(defn get-schema-ents [db]
  (dsu/touch-all '{:find [?t]
                   :where [[?t :db/ident ?ident]]}
                 db))

(defn ensure-schema
  ([] (ensure-schema (db-conn/conn)))
  ([conn]
   (let [res @(d/transact conn schema-1)
         res @(d/transact conn schema-2)
         ents (get-schema-ents (:db-after res))]
     (reset! schema-ents ents)
     res)))

(defn init []
  (ensure-schema))

;; (init)

(comment
  ;; Client-side
  (dato/transact! dato :msg-created
                  [{:db/id       (d/tempid :db.part/user)
                    :dato/guid   (d/squuid)
                    ;; ??? Should msg/at be enforced server-side?
                    :msg/at      (js/Date.)
                    :msg/user    {:db/id (:db/id me)}
                    :msg/channel {:db/id (:db/id channel)}}])

  (dato/transact! dato :msg-created
                  (let [msg-eid (d/tempid :db.part/user)]
                    [{:db/id         msg-eid
                      :dato/guid     (d/squuid)
                      ;; ??? Should msg/at be enforced server-side?
                      :msg/at        (js/Date.)
                      :user/_msgs    [{:db/id msg-eid}]
                      :channel/_msgs [{:db/id msg-eid}]}])))

(comment
  ;; msg component
  (render [_]
    (let [msg  data
          user (:user/_msgs msg)]
      (dom/li {:react-key (:db/id channel)}
              (dom/div {:class "avatar"}
                       (dom/img {:src (:user/avatar user)}))
              (:msg/body msg))))

  ;; channel component
  (render [_]
    (let [channel (d/entity db channel-eid)
          msgs    (:channel/msgs channel)]
      (dom/div
       (dom/ul
        ;; Possible filter here if search is on
        (for [msg (sort-by :msg/at msgs)]
          (om/build msg-com msg)))))))

(comment
  ;; All messages for a user
  (dsu/qes-by db :msg/_user (:db/id user))

  ;; msg component
  (render [_]
    (let [msg  data
          user (:msg/user msg)]
      (dom/li {:react-key (:db/id channel)}
              (dom/div {:class "avatar"}
                       (dom/img {:src (:user/avatar user)}))
              (:msg/body msg))))

  ;; channel component
  (render [_]
    (let [msgs (dsu/qes-by db :message/channel channel-eid)]
      (dom/div
       (dom/ul
        ;; Possible filter here if search is on
        (for [msg (sort-by :msg/at msgs)]
          (om/build msg-com msg)))))))
