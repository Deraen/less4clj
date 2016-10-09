(ns deraen.boot-less
  {:boot/export-tasks true}
  (:require [clojure.java.io :as io]
            [boot.pod :as pod]
            [boot.core :as core]
            [boot.util :as util]
            [boot.file :as file]
            [clojure.string :as string]
            [deraen.boot-less.version :refer [+version+]]))

(def ^:private deps
  [['deraen/less4clj +version+]])

(defn- find-mainfiles [fs]
  (->> fs
       core/input-files
       (core/by-ext [".main.less"])))

(defn- find-relative-path [dirs filepath]
  (if-let [file (io/file filepath)]
    (let [parent (->> dirs
                      (map io/file)
                      (some (fn [x] (if (file/parent? x file) x))))]
      (if parent (.getPath (file/relative-to parent file))))))

(defn- find-original-path [filepath]
  (let [source-paths (concat (:source-paths pod/env) (:resource-paths pod/env))
        dirs (:directories pod/env)]
    (if-let [rel-path (find-relative-path dirs filepath)]
      (or (some (fn [source-path]
                  (let [f (io/file source-path rel-path)]
                    (if (.exists f)
                      (.getPath f))))
                source-paths)
          rel-path)
      filepath)))

(defn- fixed-message
  "Replaces the tmp-path in formatted error message using path in working dir."
  [message]
  (string/replace message #"(ERROR )(file:[^\s]*)" (fn [[_ prefix wrong-path]]
                                                     (str prefix (find-original-path (.getPath (java.net.URI. wrong-path)))))))

(defn ->warning [{:keys [message source line char]}]
  {:message (some-> message
                    (fixed-message)
                    (string/replace #"\.$" ""))
   :file (if source (find-original-path (.getPath (java.net.URI. (:uri source)))))
   :line line
   :column char})

(core/deftask less
  "{less} CSS compiler.

  For each `.main.less` file in the fileset creates equivalent `.css` file.
  For example to create file `public/css/style.css` your less code should be
  at path `public/css/style.main.less`.

  If you are seeing SLF4J warnings, check https://github.com/Deraen/less4clj#log-configuration"
  [s source-map  bool "Enable source-maps for compiled CSS."
   c compression bool "Enable compression compiled CSS using simple compression."
   i inline-javascript bool "Enable inline Javascript plugin."]
  (let [output-dir  (core/tmp-dir!)
        p           (-> (core/get-env)
                        (update-in [:dependencies] into deps)
                        pod/make-pod
                        future)
        prev        (atom nil)]
    (core/with-pre-wrap fileset
      (let [less (->> fileset
                      (core/fileset-diff @prev)
                      core/input-files
                      (core/by-ext [".less"]))
            warning-meta (atom {})]
        (reset! prev fileset)
        (when (seq less)
          (util/info "Compiling {less}... %d changed files.\n" (count less))
          (doseq [f (find-mainfiles fileset)]
            (let [{:keys [warnings error]}
                  (pod/with-eval-in @p
                    (require 'less4clj.core)
                    (try
                      (less4clj.core/less-compile-to-file
                        ~(.getPath (core/tmp-file f))
                        ~(.getPath output-dir)
                        ~(core/tmp-path f)
                        {:source-map ~source-map
                         :compression ~compression
                         :inline-javascript ~inline-javascript
                         :verbosity ~(deref util/*verbosity*)})
                      (catch Exception e#
                        (let [data# (ex-data e#)]
                          (if (= :less4clj.core/error (:type data#))
                            {:error (assoc data# :message (.getMessage e#))}
                            (throw e#))) )))
                  warnings (map ->warning warnings)]

              (when error
                (throw (ex-info (fixed-message (:message error))
                                (merge {:adzerk.boot-reload/exception true}
                                       (->warning error)))))

              (swap! core/*warnings* + (count warnings))
              (swap! warning-meta update-in [(core/tmp-path f) :adzerk.boot-reload/warnings] (fnil into []) warnings)
              (doseq [{:keys [message file line column]} warnings]
                (util/warn "WARN: %s %s\n" message
                           (str (if file
                                  (str "on file "
                                       file
                                       (if line
                                         (str " at line " line " character " column))))))))))
        (-> fileset
            (core/add-resource output-dir)
            (core/add-meta @warning-meta)
            core/commit!)))))
