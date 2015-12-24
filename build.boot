(def +version+ "0.5.0-SNAPSHOT")

(set-env!
  :resource-paths #{"src" "boot-less/src" "lein-less4j/src"}
  :dependencies   '[[org.clojure/clojure "1.7.0" :scope "provided"]
                    [boot/core "2.5.2" :scope "provided"]

                    [com.github.sommeri/less4j "1.15.4"]
                    [com.github.sommeri/less4j-javascript "0.0.1" :exclusions [com.github.sommeri/less4j]]
                    [org.webjars/webjars-locator "0.29"]

                    ;; For testing the webjars asset locator implementation
                    [org.webjars/bootstrap "3.3.6" :scope "test"]])

(task-options!
  pom {:version     +version+
       :url         "https://github.com/deraen/boot-less"
       :scm         {:url "https://github.com/deraen/boot-less"}
       :license     {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}})

(defn with-files
  "Runs middleware with filtered fileset and merges the result back into complete fileset."
  [p middleware]
  (fn [next-handler]
    (fn [fileset]
      (let [merge-fileset-handler (fn [fileset']
                                    (next-handler (commit! (update fileset :tree merge (:tree fileset')))))
            handler (middleware merge-fileset-handler)
            fileset (assoc fileset :tree (reduce-kv
                                          (fn [tree path x]
                                            (if (p x)
                                              (assoc tree path x)
                                              tree))
                                          (empty (:tree fileset))
                                          (:tree fileset)))]
        (handler fileset)))))

(deftask build []
  (comp
   (pom
    :project 'deraen/less4clj)
   (with-files (fn [x]
                 (re-find #"less4clj" (:path x)))
               (comp
                (jar
                 :file (format "less4clj-%s.jar" +version+))
                (install)))

   (pom
    :project 'deraen/boot-less
    :description "Boot task to compile Less code to Css. Uses Less4j Java implementation of Less compiler."
    :dependencies [])
   (with-files (fn [x] (re-find #"boot[-_]less" (:path x)))
               (comp
                (jar
                 :file (format "boot-less-%s.jar" +version+))
                (install)))

   (pom
    :project 'deraen/lein-less4j
    :description "Leinigen task for Less4j"
    :dependencies [])
   (with-files (fn [x] (re-find #"less4j" (:path x)))
               (comp
                (jar
                 :file (format "lein-less4j-%s.jar" +version+))
                (install)))))

(deftask dev []
  (comp
   (watch)
   (repl :server true)
   (build)))

(deftask deploy []
  (comp
   (build)
   (push :repo "clojars" :gpg-sign (not (.endsWith +version+ "-SNAPSHOT")))))
