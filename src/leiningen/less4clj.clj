(ns leiningen.less4clj
  (:require [leiningen.help]
            [leiningen.core.eval :as leval]
            [leiningen.core.project :as project]
            [leiningen.core.main :as main]
            [leiningen.help :as help]
            [clojure.java.io :as io]
            [less4clj.version :refer [+version+]]))

(def less4clj-profile {:dependencies [['watchtower "0.1.1"]]})

; From lein-cljsbuild
(defn- eval-in-project [project form requires]
  (leval/eval-in-project
    project
    ; Without an explicit exit, the in-project subprocess seems to just hang for
    ; around 30 seconds before exiting. I don't fully understand why...
    `(try
       ~form
       (System/exit 0)
       (catch Exception e#
         (.printStackTrace e#)
         (System/exit 1)))
    requires))

(defn- run-compiler
  "Run the lesscss compiler."
  [project options]
  (eval-in-project
    (project/merge-profiles project [less4clj-profile])
    `(do @(main/build options))
    '(require 'less4clj.main 'less4clj.watcher)))

;; For docstrings

(defn- once
  "Compile less files once."
  [project]
  nil)

(defn- auto
  "Compile less files, then watch for changes and recompile until interrupted."
  [project]
  nil)

(defn less4clj
  "{less} CSS compiler.

For each `.main.less` file in source-paths creates equivalent `.css` file.
For example to create file `{target-path}/public/css/style.css` your less
code should be at path `{source-path}/public/css/style.main.less`.

If you are seeing SLF4J warnings, check https://github.com/Deraen/less4clj#log-configuration

Options should be provided using `:less` key in project map.

Available options:
:target-path          The path where CSS files are written to.
:source-paths         Collection of paths where LESS files are read from.
:source-map           Enable source-maps for compiled CSS.
:compression          Enable compression for compiled CSS using simple compression.
:inline-javascript    Enable inline Javascript plugin.
:verbosity            Set verbosity level, valid values are 1 and 2.

Other options are passed as is to less4clj.

Command arguments:
Add `:debug` as subtask argument to enable debugging output."
  {:help-arglists '([once auto])
   :subtasks      [#'once #'auto]}
  ([project]
   (println (help/help-for "less4clj"))
   (main/abort))
  ([project subtask & args]
   (let [args (set args)
         config (cond-> (:less project)
                  (contains? args ":debug") (assoc :verbosity 2))]
     (case subtask
       "once" (run-compiler project config)
       "auto" (run-compiler project (assoc config :auto true))
       "help" (println (help/help-for "less4clj"))
       (main/warn "Unknown task.")))))
