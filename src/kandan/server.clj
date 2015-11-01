(ns kandan.server
  (:require [bidi.bidi :as bidi]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [datomic.api :as d]
            [dato.db.utils :as dsu]
            [dato.lib.debug :as dato-debug]
            [dato.lib.server :as dato]
            [kandan.datomic.core :as db-conn]
            [kandan.config :as config]
            [kandan.session-store :as session-txn-store]
            [hiccup.core :as h]
            [immutant.codecs :as cdc]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.basic-authentication :as basic-auth]
            [ring.middleware.defaults :as ring-defaults]
            [ring.middleware.gzip :as gzip]
            [ring.middleware.multipart-params :as multipart]
            [ring.middleware.reload :as reload]
            [ring.util.response :as resp])
  (:import [java.net URLEncoder]
           [java.util UUID]))

(defn build-name->entry-file [build-name]
  (case build-name
    "pseudo"     "/js/bin-pseudo/main.js"
    "production" "/js/bin/main.js"
    "dev"        "/js/bin-debug/main.js"
    (if (config/dev?)
      "/js/bin-debug/main.js"
      "/js/bin/main.js")))

(defn bootstrap-html [params]
  ;; TODO: Add some authorization here to make sure user is allowed to
  ;; access different version of the js.
  (let [header [:head
                [:link {:href "/css/kandan.css" :rel "stylesheet" :type "text/css"}]
                [:link {:href "/highlight/css/monokai.min.css" :rel "stylesheet" :type "text/css"}]
                
                [:title "Dato • Kandan"]
                [:script {:type "text/javascript"}
                 (format "Dato = {}; Dato.config = JSON.parse('%s');" (json/encode {:dato-port (config/dato-port)}))]]]
    (h/html [:html
             header
             [:body
              [:div#kandan-app
               [:input.history {:style "display:none;"}]
               [:div.app-instance "Please wait while the app loads..."]
               [:div.debugger-container]]
              [:script {:src (build-name->entry-file (:build-name params))}]]])))

(defroutes routes
  (GET "/_source" request
    (let [path    (get-in request [:params :path])
          macros? (get-in request [:params :macros])]
      {:body (json/generate-string (dato-debug/source-path->source macros? path))}))
  (route/resources "/highlight/css/" {:root "cljsjs/common/highlight/"})
  (route/resources "/")
  (GET "/*" request
    (bootstrap-html (:params request))))

(defn authenticated? [email pass]
  false)

(def http-handler
  (as-> (var routes) routes
    (ring-defaults/wrap-defaults routes ring-defaults/api-defaults)
    (multipart/wrap-multipart-params routes)
    (gzip/wrap-gzip routes)
    (if (config/dev?)
      (reload/wrap-reload routes)
      (basic-auth/wrap-basic-authentication routes authenticated?))))

(defn run-web-server []
  (let [port (config/server-port)]
    (print "Starting web server on port" port ".\n")
    (jetty/run-jetty http-handler {:port port :join? false})))

(defn handler [{c :context}]
  (resp/redirect (str c "/index.html")))

(defn store-session-txn! [dato-state session-id txn-info]
  (session-txn-store/store-session-transition! session-id txn-info))

(defn login [dato-state session-id incoming]
  (log/infof "LOGIN! %s" incoming))

(defn logout [dato-state session-id incoming]
  (log/info "LOGOUT! %s" incoming))

(defn create-user [dato-state session-id incoming]
  (log/infof "CREATE USER! %s" incoming))

(def routing-table
  {[:kandan.user/login]     {:handler #'login}
   [:kandan.user/logout]    {:handler #'logout}
   [:kandan.user/create]    {:handler #'create-user}
   [:kandan.user/hello]     {:handler #'create-user}
   [:kandan.user/new-route] {:handler #'create-user}
   [:ss/store-session-txn!] {:handler store-session-txn!}})

(def dato-routes
  (dato/new-routing-table routing-table))

(def dato-server
  (dato/map->DatoServer {:routing-table #'dato-routes
                         :datomic-uri   db-conn/default-uri}))

(defn run []
  (dato/start! handler {:server (var dato-server)
                        :port   (config/dato-port)})
  (run-web-server))

(defn init []
  (run))

(comment
  (do
    ;; Get the intial data loaded
    (kandan.data-gen/reseed-db! (db-conn/conn))
    (require '[kandan.dev :as dev])
    (kandan.dev/browser-repl))

  ;; Dev helper function to clear channel members from ALL channels
  (reduce into (mapcat (comp (fn [[channel members]]
                                              (for [member members]
                                                (let [settings (first (filter #(= (:chanuser/user %) member) (:channel/chanusers channel)))]
                                                  (into
                                                   [[:db/retract (:db/id channel) :channel/members (:db/id member)]]
                                                   (if settings
                                                     [[:db.fn/retractEntity (:db/id settings)]]
                                                     []))))) (juxt identity :channel/members)) (dsu/qes-by (db-conn/ddb) :channel/title))))
