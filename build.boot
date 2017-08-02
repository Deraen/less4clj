(def +version+ "0.6.3-SNAPSHOT")

(set-env!
  :resource-paths #{"src" "boot-less/src" "lein-less4j/src"}
  :source-paths #{"test" "test-resources"}
  :dependencies   '[[org.clojure/clojure "1.8.0" :scope "provided"]
                    [metosin/boot-alt-test "0.3.2" :scope "test"]
                    ;; Webjars-locator uses logging
                    [org.slf4j/slf4j-nop "1.7.25" :scope "test"]

                    [com.github.sommeri/less4j "1.17.2"]
                    [com.github.sommeri/less4j-javascript "0.0.1" :exclusions [com.github.sommeri/less4j]]
                    [org.webjars/webjars-locator "0.32-1"]

                    ;; For testing the webjars asset locator implementation
                    [org.webjars/bootstrap "3.3.7-1" :scope "test"]])

(require '[metosin.boot-alt-test :refer [alt-test]])

(task-options!
  pom {:version     +version+
       :url         "https://github.com/deraen/less4clj"
       :scm         {:url "https://github.com/deraen/less4clj"}
       :license     {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}})

(defn with-files
  "Runs middleware with filtered fileset and merges the result back into complete fileset."
  [p middleware]
  (fn [next-handler]
    (fn [fileset]
      (let [merge-fileset-handler (fn [fileset']
                                    (next-handler (commit! (assoc fileset :tree (merge (:tree fileset) (:tree fileset'))))))
            handler (middleware merge-fileset-handler)
            fileset (assoc fileset :tree (reduce-kv
                                          (fn [tree path x]
                                            (if (p x)
                                              (assoc tree path x)
                                              tree))
                                          (empty (:tree fileset))
                                          (:tree fileset)))]
        (handler fileset)))))

(deftask write-version-file
  [n namespace NAMESPACE sym "Namespace"]
  (let [d (tmp-dir!)]
    (fn [next-handler]
      (fn [fileset]
        (let [f (clojure.java.io/file d (-> (name namespace)
                                            (clojure.string/replace #"\." "/")
                                            (clojure.string/replace #"-" "_")
                                            (str ".clj")))]
          (clojure.java.io/make-parents f)
          (spit f (format "(ns %s)\n\n(def +version+ \"%s\")" (name namespace) +version+)))
        (next-handler (-> fileset (add-resource d) commit!))))))

(deftask build []
  (comp
   (with-files
    (fn [x] (re-find #"less4clj" (tmp-path x)))
    (comp
     (pom
      :project 'deraen/less4clj
      :description "Clojure wrapper for Less4j.")
     (jar)
     (install)))

   (with-files
    (fn [x] (re-find #"boot_less" (tmp-path x)))
    (comp
     (pom
      :project 'deraen/boot-less
      :description "Boot task to compile {less}"
      :dependencies [])
     (write-version-file :namespace 'deraen.boot-less.version)
     (jar)
     (install)))

   (with-files
    (fn [x] (re-find #"leiningen" (tmp-path x)))
    (comp
     (pom
      :project 'deraen/lein-less4j
      :description "Leinigen task to compile {less}"
      :dependencies [])
     (write-version-file :namespace 'leiningen.less4j.version)
     (jar)
     (install)))))

(deftask dev []
  (comp
   (watch)
   (repl :server true)
   (build)
   (target)))

(deftask deploy []
  (comp
   (build)
   (push :repo "clojars" :gpg-sign (not (.endsWith +version+ "-SNAPSHOT")))))

(ns-unmap *ns* 'test)

(deftask test []
  (comp
    (write-version-file :namespace 'deraen.boot-sass.version)
    (write-version-file :namespace 'leiningen.sass4clj.version)
    (alt-test :report 'eftest.report.pretty/report)))

(deftask autotest []
  (comp
    (watch)
    (test)))
