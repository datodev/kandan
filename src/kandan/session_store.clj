(ns kandan.session-store
  (:import [java.io BufferedWriter FileWriter]))

(def logfile
  (agent (BufferedWriter. (FileWriter. "shared-logfile.log"))))

(defn write-out [file out msg]
  (let [f (BufferedWriter. (FileWriter. "shared-logfile.log"))]
    (.write out msg)
    out))

(defn log [logger msg]
  (send logger write-out msg))

(defn close [logger]
  (send logger #(.close %)))

(defn store-session-transition! [session-id txn-info]
  (log ))

(log logfile "My awesome message\n")
(log logfile "My Other awesome message\n")
