(ns leiningen.less4j
  (:require [leiningen.help]
            [leiningen.core.eval :as leval]
            [leiningen.core.project :as project]
            [leiningen.core.main :as main]
            [leiningen.help :as help]
            [clojure.java.io :as io]
            [leiningen.less4j.version :refer [+version+]]))

(defn main-file? [file]
  (.endsWith (.getName file) ".main.less"))

(defn find-main-files [source-paths]
  (mapcat (fn [source-path]
            (let [file (io/file source-path)]
              (->> (file-seq file)
                   (filter main-file?)
                   (map (fn [x] [(.getPath x) (.toString (.relativize (.toURI file) (.toURI x)))])))))
          source-paths))

(def less4j-profile {:dependencies [['deraen/less4clj +version+]
                                    ['watchtower "0.1.1"]]})

; From lein-cljsbuild
(defn- eval-in-project [project form requires]
  (leval/eval-in-project
    project
    ; Without an explicit exit, the in-project subprocess seems to just hang for
    ; around 30 seconds before exiting.  I don't fully understand why...
    `(try
       (do
         ~form
         (System/exit 0))
       (catch Exception e#
         (do
           (if (= :less4clj.core/error (:type (ex-data e#)))
             (println (.getMessage e#))
             (.printStackTrace e#))
           (System/exit 1))))
    requires))

(defn- run-compiler
  "Run the lesscss compiler."
  [project
   {:keys [source-paths target-path]
    :as options}
   watch?]
  (when-not target-path
    (main/abort "Lein-less4j requires :target-path option."))
  (let [project' (project/merge-profiles project [less4j-profile])
        main-files (vec (find-main-files source-paths))]
    (eval-in-project
      project'
      `(let [f# (fn compile-less [& ~'_]
                  (doseq [[path# relative-path#] ~main-files]
                    (println (format "Compiling {less}... %s" relative-path#))
                    (let [result#
                          (try
                            (less4clj.core/less-compile-to-file
                              path#
                              ~(.getPath (io/file target-path))
                              relative-path#
                              ~(dissoc options :target-path :source-paths))
                            (catch Exception e#
                              (if ~watch?
                                (println (.getMessage e#))
                                (throw e#))))]
                      (doseq [warning# (:warnings result#)]
                        (println (format "WARN: %s %s\n" (:message warning#)
                                         (str (if (:uri (:source warning#))
                                                (str "on file "
                                                     (:uri (:source warning#))
                                                     (if (:line warning#)
                                                       (str " at line " (:line warning#) " character " (:char warning#))))))))))))]
         (if ~watch?
           @(watchtower.core/watcher
             ~source-paths
             (watchtower.core/rate 100)
             (watchtower.core/file-filter watchtower.core/ignore-dotfiles)
             (watchtower.core/file-filter (watchtower.core/extensions :less))
             (watchtower.core/on-change f#))
           (f#)))
      '(require 'less4clj.core 'watchtower.core))))

;; For docstrings

(defn- once
  "Compile less files once."
  [project]
  nil)

(defn- auto
  "Compile less files, then watch for changes and recompile until interrupted."
  [project]
  nil)

(defn less4j
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
   (println (help/help-for "less4j"))
   (main/abort))
  ([project subtask & args]
   (let [args (set args)
         config (cond-> (:less project)
                  (contains? args ":debug") (assoc :verbosity 2))]
     (case subtask
       "once" (run-compiler project config false)
       "auto" (run-compiler project config true)
       "help" (println (help/help-for "less4j"))
       (main/warn "Unknown task.")))))
