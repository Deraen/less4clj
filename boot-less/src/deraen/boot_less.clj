(ns deraen.boot-less
  {:boot/export-tasks true}
  (:require [clojure.java.io :as io]
            [boot.pod :as pod]
            [boot.core :as core]
            [boot.util :as util]
            [deraen.boot-less.version :refer [+version+]]))

(def ^:private deps
  [['deraen/less4clj +version+]])

(defn- find-mainfiles [fs]
  (->> fs
       core/input-files
       (core/by-ext [".main.less"])))

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
                      (core/by-ext [".less"]))]
        (reset! prev fileset)
        (when (seq less)
          (util/info "Compiling {less}... %d changed files.\n" (count less))
          (doseq [f (find-mainfiles fileset)]
            (let [{:keys [warnings]}
                  (pod/with-call-in @p
                      (less4clj.core/less-compile-to-file
                        ~(.getPath (core/tmp-file f))
                        ~(.getPath output-dir)
                        ~(core/tmp-path f)
                        {:source-map ~source-map
                         :compression ~compression
                         :inline-javascript ~inline-javascript
                         :verbosity ~(deref util/*verbosity*)}))]
              (swap! core/*warnings* + (count warnings))
              (doseq [{:keys [message source line char]} warnings]
                (util/warn "WARN: %s %s\n" message
                           (str (if (:uri source)
                                  (str "on file "
                                       (:uri source)
                                       (if line
                                         (str " at line " line " character " char)))))))))))
        (-> fileset
            (core/add-resource output-dir)
            core/commit!))))
