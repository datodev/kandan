(ns kandan.client.components.root
  (:require [clojure.string :as string]
            [datascript :as d]
            [dato.db.utils :as dsu]
            [dato.lib.controller :as con]
            [dato.lib.core :as dato]
            [dato.lib.db :as db]
            [kandan.client.routes :as routes]
            [kandan.client.utils :as utils]
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

(def users
  [{:user/disabled?   false
    :user/email       "sean@bushi.do"
    :user/nick        "sgrove"
    :user/given-name  "Sean"
    :user/family-name "Grove"
    :user/avatar      "https://secure.gravatar.com/avatar/767934a648524da57388558217ad9c2d?s=25&d=identicon"}
   {:user/disabled?   false
    :user/email       "sean@bushi.do"
    :user/nick        "dww"
    :user/given-name  "Sean"
    :user/family-name "Grove"
    :user/avatar      "https://secure.gravatar.com/avatar/767934a648524da57388558217ad9c2d?s=25&d=identicon"}
   {:user/disabled?   false
    :user/email       "sean@bushi.do"
    :user/nick        "sgdesign"
    :user/given-name  "Sean"
    :user/family-name "Grove"
    :user/avatar      "https://secure.gravatar.com/avatar/767934a648524da57388558217ad9c2d?s=25&d=identicon"}
   {:user/disabled?   false
    :user/email       "sean@bushi.do"
    :user/nick        "mrab"
    :user/given-name  "Sean"
    :user/family-name "Grove"
    :user/avatar      "https://secure.gravatar.com/avatar/767934a648524da57388558217ad9c2d?s=25&d=identicon"}])

(defcomponent tab [data owner opts]
  (display-name [_]
    "Tab")
  (render [_]
    (let [channel  data
          loading? false]
      (dom/li
       {:key   (:channel/title data)
        :class (str "protected "
                    ;;(utils/safe-sel (:channel/title data))
                    (when (:selected? channel) " active"))}
       (dom/a
        {:class    "show_channel"
         :on-click (fn [event]
                     (kill! event)
                     (js/console.log "tab-selected"))}
        (:channel/title channel)
        (when loading?
          (dom/i {:class "icon-spinner icon-spin"})))))))

(defcomponent navbar [data owner opts]
  (display-name [_]
    "Navbar")
  (render [_]
    (let [{:keys [dato]} (om/get-shared owner)
          db             (dato/db dato)
          channels       (dsu/qes-by db :channel/title)]
      (dom/nav
       {:class "nav"}
       (dom/form
        {:class     "search"
         :action    "/search"
         :method    "get"
         :on-submit (constantly false)}
        (dom/input
         {:class     "query"
          :type      "text"
          :on-focus  #(js/console.log "global-search-focused")
          :on-blur   #(js/console.log "global-search-blurred")
          :on-key-up #(js/console.log "global-search-updated")})
        (dom/input {:class       "submit"
                    :placeholder "Search"
                    :type        "submit"}))
       (dom/ul
        {:id "channel_nav"}
        (for [tab-data (sort-by :channel/title channels)]
          (do
            (js/console.log "tab data " tab-data)
            (om/build tab tab-data)))
        (dom/li
         {:key      "new-tab"
          :on-click (js/console.log "channel-created")}
         (dom/a {:class    "create_channel"
                 :href     "#"
                 :on-click (fn [event]
                             (kill! event)
                             (js/console.log "channel-created"))}
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
  (or (:user/nick user-ent)
      (:user/email user-ent)))

(defcomponent msg [data owner opts]
  (render [_]
    (let [msg data]
      (dom/div
       {:class "activity"}
       (dom/time {:class "posted_at"} (:msg/created-at msg))
       (dom/img {:class "avatar"
                 :src   (get-in msg [:msg/user :user/avatar] "https://secure.gravatar.com/avatar/704ca0ef793c7d10d2c75e0286a5d36b?s=30&d=identicon")})
       (dom/div
        {:class "readable"}
        (dom/span
         {:class "user"}
         (display-name (:msg/user msg)))
        (dom/span
         {:class "content"}
         (:msg/body msg)))))))

(defcomponent channel [data owner opts]
  (render [_]
    (let [{:keys [dato]} (om/get-shared owner)
          db             (dato/db dato)
          channels       (dsu/qes-by db :channel/title)
          channel        (d/entity db (:db/id (first channels)))]
      (dom/div
       {:id    "channels-1"
        :class "channels-pane active"}
       (dom/div
        {:class "paginated-activities"}
        (dom/div
         {:class "pagination"
          :style {:display "none"}}
         (dom/i {:class "icon-spinner icon-spin icon-2x"})
         "Loading previous messages")
        (dom/div
         {:class "channel-activities"}
         (for [msg-data (:channel/msgs channel)]
           (om/build msg msg-data))))
       (dom/div
        {:class "chatbox"}
        (dom/textarea {:class "chat-input"})
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
  (render [_]
    (let [me {:user/disabled?   false
              :user/email       "sean@bushi.do"
              :user/nick        "sgrove"
              :user/given-name  "Sean"
              :user/family-name "Grove"
              :user/avatar      "https://secure.gravatar.com/avatar/767934a648524da57388558217ad9c2d?s=25&d=identicon"}]
      (dom/div
       {:class "header user-header open-menu"}
       (dom/a
        {:class "user-menu-toggle "}
        (dom/img
         {:src (:user/avatar me)}
         (dom/i
          {:class "icon-angle button right"
           :style {:height 24}})
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
  (render [_]
    (let [user users]
      (widget "/images/people_icon.png" "People"
              (dom/ul
               {:class "user_list"}
               (for [user users]
                 (dom/li
                  {:class "user"
                   :title (:user/nick user)}
                  (dom/img
                   {:class "avatar"
                    :src (:user/avatar user)})
                  (display-name user))))))))

(defcomponent notifications-widget [data owner opts]
  (render [_]
    (let [user users]
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
                 (dom/span))))))))

(defcomponent media-widget [data owner opts]
  (render [_]
    (let [files        [{:file/name    "Penguins.jpg"
                         :file/comment "Some penguins"
                         :file/failed? false
                         :file/mime    :file.type/image}
                        {:file/name    "Snip20151010_5.png"
                         :file/comment "Some penguins"
                         :file/failed? false
                         :file/mime    :file.type/image}
                        {:file/name    "LICENSE"
                         :file/comment "Another license"
                         :file/failed? false
                         :file/mime    :file.type/unknown}
                        {:file/name    "LICENSE"
                         :file/comment "Some license"
                         :file/failed? false
                         :file/mime    :file.type/image}]
          mime->icon   {:file.type/image "/images/img_icon.png"}
          default-icon "/images/file_icon.png"]
      (widget "/images/media_icon.png" "Media"
              (dom/ul
               {:class "file_list"}
               (for [file files]
                 (dom/li
                  {:class "file_item"}
                  (dom/a
                   {:target "_blank"
                    :href   "/whatever"}
                   (dom/img
                    {:src (get mime->icon (:file/mime file) default-icon)})
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
     (let [{:keys [dato]}   (om/get-shared owner)
           db               (dato/db dato)
           transact!        (partial dato/transact! dato)
           me               (dato/me db)
           session          (dato/local-session db)
           task-filter      (:session/task-filter session)
           pred             (case task-filter
                              :completed :task/completed?
                              :active    (complement :task/completed?)
                              (constantly true))
           all-tasks        (dsu/qes-by db :task/title)
           grouped          (group-by :task/completed? all-tasks)
           active-tasks     (get grouped false)
           completed-tasks  (get grouped true)
           shown-tasks      (->> all-tasks
                                 (filter pred)
                                 (sort-by :task/order))]
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
        (om/build navbar {}))))))
