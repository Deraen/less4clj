(ns deraen.boot-less
  {:boot/export-tasks true}
  (:require
   [clojure.java.io :as io]
   [boot.pod        :as pod]
   [boot.core       :as core]
   [boot.util       :as util]))

(def ^:private deps
  '[[deraen/less4clj "0.4.1"]])

(defn- find-mainfiles [fs]
  (->> fs
       core/input-files
       (core/by-ext [".main.less"])))

(core/deftask less
  "Compile Less code."
  [s source-map  bool "Create source-map for compiled CSS."
   c compression bool "Compress compiled CSS using simple compression."
   i inline-javascript bool "Enable inline Javascript plugin"]
  (let [output-dir  (core/tmp-dir!)
        p           (-> (core/get-env)
                        (update-in [:dependencies] into deps)
                        pod/make-pod
                        future)
        last-less   (atom nil)]
    (core/with-pre-wrap fileset
      (let [less (->> fileset
                      (core/fileset-diff @last-less)
                      core/input-files
                      (core/by-ext [".less"]))]
        (reset! last-less fileset)
        (when (seq less)
          (util/info "Compiling {less}... %d changed files.\n" (count less))
          (doseq [f (find-mainfiles fileset)]
            (pod/with-call-in @p
              (less4clj.core/less-compile-to-file
                ~(.getPath (core/tmp-file f))
                ~(.getPath output-dir)
                ~(core/tmp-path f)
                {:source-map ~source-map
                 :compression ~compression
                 :inline-javascript ~inline-javascript
                 :verbosity ~(deref util/*verbosity*)})))))
        (-> fileset
            (core/add-resource output-dir)
            core/commit!))))
