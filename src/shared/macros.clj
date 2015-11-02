(ns macros
  (:require [clojure.java.shell :as shell]
            [clojure.string :as string]))

(defmacro get-git-version []
  (-> (shell/sh "bash" "-c" "git log --oneline -n 1")
         :out
         (string/split #" ")
         first))
