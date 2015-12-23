(defproject deraen/less4clj "0.4.0"
  :description "Wrapper for Less4j"
  :url "https://github.com/deraen/less4clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[org.clojure/clojure "1.7.0" :scope "provided"]
                 [com.github.sommeri/less4j "1.15.4"]
                 [com.github.sommeri/less4j-javascript "0.0.1" :exclusions [com.github.sommeri/less4j]]
                 [org.webjars/webjars-locator "0.29"]
                 ;; FIXME: Will this cause problems if there is another
                 ;; slf4j implementation already present?
                 [org.slf4j/slf4j-nop "1.7.13"]

                 ;; For testing the webjars asset locator implementation
                 [org.webjars/bootstrap "3.3.6" :scope "test"]]
  :profiles {:dev {:resource-paths ["test-resources"]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0-RC4"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}}
  :aliases {"all" ["with-profile" "dev:dev,1.6:dev,1.8"]})
