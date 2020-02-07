(ns less4clj.api-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [less4clj.api :as less])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(defn- temp-dir
  [prefix]
  (.toString (Files/createTempDirectory prefix (into-array FileAttribute []))))

(def ^:private includer-less
  "body {
  color: red;
}

@import 'includee';
")

(def ^:private includee-less
  "p {
  color: blue;
}")

(deftest include-paths
  (let [input-dir (temp-dir "less4clj-input")
        include-dir (temp-dir "less4clj-include")
        output-dir (temp-dir "less4clj-output")
        options {:source-paths [input-dir include-dir]
                 :inputs ["includer.less"]
                 :target-path output-dir}]
    (spit (io/file input-dir "includer.less") includer-less)
    (spit (io/file include-dir "includee.less") includee-less)
    (less/build options)
    (is (= "body {\n  color: red;\n}\np {\n  color: blue;\n}\n"
           (slurp (io/file output-dir "includer.css"))))))

