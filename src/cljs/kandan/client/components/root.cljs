(ns kandan.client.components.root
  (:require [clojure.string :as string]
            [datascript :as d]
            [dato.db.utils :as dsu]
            [dato.lib.controller :as con]
            [dato.lib.core :as dato]
            [dato.lib.db :as db]
            [goog.crypt :as crypt]
            [goog.crypt.Md5]
            [kandan.client.components.plugins :as plugins]
            [kandan.client.datetime :as dt]
            [kandan.client.db :as kdb]
            [kandan.client.routes :as routes]
            [kandan.client.utils :as utils]
            [kandan.client.components.utils :as com-utils]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom]
            [sablono.core :as html :refer-macros [html]]))

(defn kill! [event]
  (doto event
    (.preventDefault)
    (.stopPropagation)))

(defn delay-focus!
  "Waits 20ms (enough time to queue up a rerender usually, but
  racey) and then focus an input"
  ([root selector]
   (delay-focus! root selector false))
  ([root selector select?]
   (js/setTimeout #(when-let [input (utils/sel1 root selector)]
                     (.focus input)
                     (when select?
                       (.select input))) 20)))

(defn email->gravatar-url [email]
  (let [email (or email "unknown-email@unknown-domain.com")
        container (doto (goog.crypt.Md5.)
                    (.update email))
        hash (crypt/byteArrayToHex (.digest container))]
    (str "http://gravatar.com/avatar/" hash "?s=30&d=identicon")))

(defcomponent tab [data owner opts]
  (display-name [_]
    "Tab")
  (did-mount [_]
    (let [conn (dato/conn (om/get-shared owner :dato))]
      (db/refresh-on! owner conn [[:attr :session/inspected]])))
  (render [_]
    (let [{:keys [dato]} (om/get-shared owner)
          db             (dato/db dato)
          me             (kdb/me db)
          channel        data
          session        (kdb/local-session db)
          selected       (kdb/inspected-channel db session)
          loading?       false]
      (dom/li
       {:key   (:channel/title data)
        :class (str "protected "
                    ;;(utils/safe-sel (:channel/title data))
                    (when (= (:db/id selected) (:db/id channel)) " active"))}
       (dom/a
        {:class    "show_channel"
         :href     "#"
         :on-click (fn [event]
                     (kill! event)
                     (dato/transact! dato :channel-selected
                                     [{:db/id             (:db/id session)
                                       :session/inspected (:db/id channel)}]))}
        (:channel/title channel)
        (when loading?
          (dom/i {:class "icon-spinner icon-spin"})))))))

(defcomponent navbar [data owner opts]
  (did-mount [_]
    (let [conn (dato/conn (om/get-shared owner :dato))]
      (js/window.setInterval #(om/refresh! owner) 500)
      (db/refresh-on! owner conn [[:attr :channel/title]])))
  (display-name [_]
    "Navbar")
  (render [_]
    (let [{:keys [dato]} (om/get-shared owner)
          db             (dato/db dato)
          me             (kdb/me db)
          channels       (dsu/qes-by db :channel/title)
          l-state        (dato/get-state owner)]
      (dom/nav
       {:class (str "nav"
                    (when (:search/focused? l-state)
                      " search-focus"))}
       (dom/form
        {:class     "search"
         :action    "/search"
         :method    "get"
         :on-submit (constantly false)}
        (dom/input
         {:class    "query"
          :type     "text"
          :on-focus #(dato/set-state! owner {:search/focused? true})
          :on-blur  #(dato/set-state! owner {:search/focused? false})
          :on-input #(let [new-value (.. % -target -value)]
                       (when (not= (:search/value l-state) new-value)
                         (dato/set-state! owner {:search/value (.. % -target -value)})))})
        (dom/input {:class       "submit"
                    :placeholder "Search"
                    :type        "submit"}))
       (dom/ul
        {:id "channel_nav"}
        (for [tab-data (sort-by :channel/title channels)]
          (om/build tab tab-data))
        (dom/li
         {:key "new-tab"}
         (dom/a {:class    "create_channel"
                 :href     "#"
                 :on-click (fn [event]
                             (kill! event)
                             (when-let [title (js/prompt "New channel title")]
                               (let [channel-eid (d/tempid :db.part/user)]
                                 (dato/transact! dato :channel-created
                                                 [{:db/id           channel-eid
                                                   :dato/guid       (d/squuid)
                                                   :channel/title   title
                                                   :channel/admins  [{:db/id (:db/id me)}]
                                                   :channel/members [{:db/id (:db/id me)}]
                                                   :org/_channels   {:db/id (-> me :org/_users first :db/id)}}]
                                                 {:tx/persist? true}))))}
                (dom/strong "+"))))))))

(defmethod con/transition :server/find-tasks-succeeded
  [db {:keys [data] :as args}]
  ;; The pull request is insertable as-is, so we don't need to do any
  ;; pre-processing, just return the results.
  ;;
  ;; XXX: We're not picking up on the ref attr properly, so it's not
  ;; actually insertable as-is. This can probably be moved love down
  ;; the Dato stack. Either way, Preprocessing step for ref-attrs (to
  ;; handle the diff between DS/Datomic re: enums) needs to be
  ;; eliminated.
  (let [results (:results data)]
    (mapv (fn [entity-like]
            (->> entity-like
                 (map (fn [[k v]]
                        (if (and (dsu/ref-attr? db k)
                                 (keyword? v))
                          [k (db/enum-id db v)]
                          [k v])))
                 (into {}))) results)))

(defmethod con/effect! :server/find-tasks-succeeded
  ;; Router is from the context we passed in when instantiating our
  ;; app in core.cljs
  [{:keys [router] :as context} old-db new-db exhibit]
  (routes/start! router))

(defn display-name [user-ent]
  (or (when (and (:user/given-name user-ent) (:user/family-name user-ent))
        (str (:user/given-name user-ent) " " (:user/family-name user-ent)))
      (:user/nick user-ent)
      (:user/email user-ent)))

(def delimiter-re
  #" ")

(defn expand-msg [current-user msg]
  (let [members (get-in msg [:channel/_msgs :channel/members])]
    (let [content (-> (string/split (:msg/body msg) delimiter-re)
                      plugins/code
                      plugins/pastie
                      (plugins/mentions members)
                      (plugins/slash-me current-user members)
                      plugins/slash-play
                      plugins/emoticons
                      plugins/image-embed
                      plugins/youtube-embed
                      plugins/vimeo-embed
                      plugins/links
                      plugins/rgb-embed
                      plugins/hex-embed)]
      (interpose " " content))))

(defcomponent msg [data owner opts]
  (render [_]
    (let [{:keys [dato]} (om/get-shared owner)
          db             (dato/db dato)
          me             (kdb/me db)
          msg            data
          channel        (d/entity db (:channel/_msgs msg))
          members        (:channel/members channel)]
      (dom/div
       {:class "activity"}
       (dom/time
        {:class "posted_at"}
        (dt/time-ago (:msg/at msg)) " ago")
       (dom/img {:class "avatar"
                 :src   (or (get-in msg [:msg/user :user/avatar])
                            (email->gravatar-url (get-in msg [:msg/user :user/email])))})
       (dom/div
        {:class "readable"}
        (dom/span
         {:class "user"}
         (display-name (:msg/user msg)))
        (dom/span
         {:class "content"}
         (expand-msg me msg)))))))

(defcomponent channel [data owner opts]
  (did-mount [_]
    (let [conn                   (dato/conn (om/get-shared owner :dato))
          node                   (om/get-node owner)
          message-area           (.querySelector node ".paginated-activities")]
      (db/refresh-on! owner conn [[:attr :session/inspected]
                                  [:attr :channel/msgs]])))
  (did-update [_ _ _]
              (let [node                (om/get-node owner)
                    message-area        (.querySelector node ".paginated-activities")
                    bottom              (- (.-scrollHeight message-area)
                                           (.-offsetHeight message-area))
                    scrolled-to-bottom? (== (.-scrollTop message-area) bottom)]
                ;; TODO: Only scroll when appropriate
                (set! (.-myNode js/window) message-area)
                (set! (.-scrollTop message-area) bottom)))
  (render [_]
    (let [{:keys [dato]} (om/get-shared owner)
          db             (dato/db dato)
          me             (kdb/me db)
          session        (kdb/local-session db)
          channel        (kdb/inspected-channel db session)
          msgs           (->> (:channel/msgs channel)
                              (sort-by :msg/at)
                              (take-last 10))]
      (dom/div
       {:class "channels-pane active"}
       (dom/div
        {:class "paginated-activities"}
        (dom/div
         {:class "pagination"
          :style {:display "none"}}
         (dom/i {:class "icon-spinner icon-spin icon-2x"})
         "Loading previous messages")
        (dom/div
         {:class "channel-activities"}
         (for [msg-data msgs]
           (om/build msg msg-data))))
       (dom/div
        {:class "chatbox"}
        (dom/textarea
         {:class     "chat-input"
          :on-key-up (fn [event]
                       (when (and (com-utils/enter? event)
                                  (not (com-utils/shift? event)))
                         (let [msg-eid  (d/tempid :db.part/user)
                               msg-guid (d/squuid)]
                           (dato/transact! dato :msg-created
                                           [{:db/id         msg-eid
                                             :dato/guid     msg-guid
                                             :dato/type     {:db/id (db/enum-id db :kandan.type/msg)}
                                             :msg/body      (.. event -target -value)
                                             :msg/user      {:db/id (:db/id me)}
                                             :msg/at        (js/Date.)
                                             :channel/_msgs {:db/id (:db/id channel)}}]
                                           {:tx/persist? true}))
                         (set! (.-value (.-target event)) "")))})
        (dom/button
         {:class "post"}
         "Post"))))))

;; TODO: This will get killed with the new design
(defcomponent main-area [data owner opts]
  (render [_]
    (dom/article
     {:class "main-area"}
     (dom/header
      {:class "header"}
      (dom/a
       {:class "logo"
        :href  "/"}
       (dom/img
        {:alt "Logo"
         :src "/images/logo-4697e3041701369b6310a0c2c2bdf106.png"})))
     (dom/div
      {:id "channels"}
      (om/build channel {})))))

(defcomponent user-menu [data owner opts]
  (did-mount [_]
    (let [conn (dato/conn (om/get-shared owner :dato))]
      (db/refresh-on! owner conn [[:attr :user/me?]
                                  [:attr :user/nick]
                                  [:attr :user/email]
                                  [:attr :user/given-name]
                                  [:attr :user/family-name]])))
  (render [_]
    (let [{:keys [dato]} (om/get-shared owner)
          db             (dato/db dato)
          session        (kdb/local-session db)
          channel        (kdb/inspected-channel db session)
          me             (kdb/me db)
          l-state        (dato/get-state owner)]
      (dom/div
       ;; change to open-menu to open the menu
       {:class (str "header user-header"
                    (when (:menu/open? l-state)
                      " open-menu"))}
       (dom/a
        {:class    "user-menu-toggle "
         :href     "#"
         :on-click (fn [event]
                     (kill! event)
                     (dato/set-state! owner {:menu/open? (not (:menu/open? l-state))}))}
        (dom/img
         {:src (or (:user/avatar me)
                   (email->gravatar-url (:user/email me)))}
         (dom/i
          {:class "icon-angle button right"
           :style {:height 24}})
         (js/console.log "me " (when me (dsu/t me)))
         (display-name me)))
       (dom/ul
        {:class "user-menu"}
        (dom/li)
        (dom/li
         (dom/a
          {:href "/users/edit"
           :on-click (fn [event] (kill! event))}
          "Edit Account"))
        (dom/li
         (dom/a
          {:href "/users/sign_out"
           :on-click (fn [event] (kill! event))}
          "Logout"))
        (dom/li
         (dom/a
          {:href "/about"
           :on-click (fn [event] (kill! event))}
          "About Kandan")))))))

(defn widget
  ([img-src title body]
   (widget img-src title body nil))
  ([img-src title body action]
   (dom/div
    {:class "widget"}
    (dom/h5
     {:class "widget-header"}
     (dom/img {:src img-src})
     title)
    (dom/div
     {:class "widget-content"}
     body)
    (when action
      (dom/div
       {:class "widget-action-bar"}
       action)))))

(defcomponent people-widget [data owner opts]
  (did-mount [_]
    (let [conn (dato/conn (om/get-shared owner :dato))]
      (db/refresh-on! owner conn [[:attr :session/inspected]
                                  [:attr :channel/members]])))
  (render [_]
    (let [{:keys [dato]} (om/get-shared owner)
          db             (dato/db dato)
          session        (kdb/local-session db)
          channel        (kdb/inspected-channel db session)
          users          (:channel/members channel)]
      (widget "/images/people_icon.png" "People"
              (dom/ul
               {:class "user_list"}
               (for [user users]
                 (dom/li
                  {:class "user"
                   :title (:user/nick user)}
                  (dom/img
                   {:class "avatar"
                    :src (or (:user/avatar user)
                             (email->gravatar-url (:user/email user)))})
                  (display-name user))))))))

(defcomponent notifications-widget [data owner opts]
  (did-mount [_]
    (let [conn (dato/conn (om/get-shared owner :dato))]
      (db/refresh-on! owner conn [[:attr :session/inspected]])))
  (render [_]
    (widget "/images/people_icon.png" "Notifications"
            (dom/ul
             {:class "notifications_list"}
             (dom/li
              {:class "notification popup-notifications"}
              (dom/label
               (dom/input
                {:class "switch"
                 :type  "checkbox"}
                "Desktop Notifications")
               (dom/span)))
             (dom/li
              {:class "notification popup-notifications"}
              (dom/label
               (dom/input
                {:class   "switch"
                 :checked true
                 :type    "checkbox"}
                "Sounds")
               (dom/span)))))))

(defcomponent media-widget [data owner opts]
  (did-mount [_]
    (let [conn (dato/conn (om/get-shared owner :dato))]
      (db/refresh-on! owner conn [[:attr :session/inspected]
                                  [:attr :channel/files]])))
  (render [_]
    (let [{:keys [dato]} (om/get-shared owner)
          db             (dato/db dato)
          session        (kdb/local-session db)
          channel        (kdb/inspected-channel db session)
          files          (:channel/files channel)
          mime->icon     {:file.type/image "/images/img_icon.png"
                          :file.type/audio "/images/audio_icon.png"
                          :file.type/video "/images/video_icon.png"
                          :file.type/pdf   "/images/media_icon.png"}
          default-icon   "/images/file_icon.png"]
      (js/console.log "files: " (map dsu/t files))
      (widget "/images/media_icon.png" "Media"
              (dom/ul
               {:class "file_list"}
               (for [file files]
                 (dom/li
                  {:class "file_item"}
                  (dom/a
                   {:target "_blank"
                    :href   (:file/src file)}
                   (dom/img
                    {:src (get mime->icon (db/enum db (:file/type file)) default-icon)})
                   (:file/name file)))))
              (dom/form
               {:id             "file_upload"
                :accept-charset "UTF-8"
                :action         "/attachments"
                :method         "post"}
               (dom/div
                {:id "dropzone"}
                "Drop file here to uplaod"))))))

(defcomponent root-com [data owner opts]
  (display-name [_]
    "Kandan")
  (did-mount [_]
    (d/listen! (dato/conn (om/get-shared owner :dato)) :dato-root #(om/refresh! owner)))
  (will-unmount [_]
    (d/unlisten! (dato/conn (om/get-shared owner :dato)) :dato-root))
  (render [_]
    (html
     (let [{:keys [dato]} (om/get-shared owner)
           db             (dato/db dato)
           me             (kdb/me db)]
       (if me
         (dom/div
          {:class ""}
          (dom/aside
           {:class "sidebar"}
           (om/build user-menu {})
           (dom/div
            {:class "widgets"}
            (om/build people-widget {})
            (om/build notifications-widget {})
            (om/build media-widget {})))
          (om/build main-area {})
          (om/build navbar {}))
         (dom/div "Loading..."))))))
