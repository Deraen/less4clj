(set-env!
  :resource-paths #{"src"}
  :dependencies   '[[org.clojure/clojure "1.6.0"       :scope "provided"]
                    [boot/core           "2.5.2"       :scope "provided"]
                    [deraen/less4clj     "0.4.0"       :scope "test"]])

(def +version+ "0.4.4")

(task-options!
  pom {:project     'deraen/boot-less
       :version     +version+
       :description "Boot task to compile Less code to Css. Uses Less4j Java implementation of Less compiler."
       :url         "https://github.com/deraen/boot-less"
       :scm         {:url "https://github.com/deraen/boot-less"}
       :license     {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask build []
  (comp
   (pom)
   (jar)
   (install)))

(deftask dev []
  (comp
   (watch)
   (repl :server true)
   (build)))

(deftask deploy []
  (comp
   (build)
   (push :repo "clojars" :gpg-sign (not (.endsWith +version+ "-SNAPSHOT")))))
