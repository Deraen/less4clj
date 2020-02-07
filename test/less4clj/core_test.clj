(ns less4clj.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [less4clj.core :refer [normalize-url join-url less-compile]]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.io File]
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(deftest normalize-url-test
  (is (= "foo/bar" (normalize-url "foo/./bar")))
  (is (= "foo/bar" (normalize-url "foo//bar")))
  (is (= "bar" (normalize-url "foo/../bar")))
  (is (= "../foo" (normalize-url "../foo")))
  (is (= "../../foo" (normalize-url "../../foo")))
  (is (= "../../../foo" (normalize-url "../../../foo")))
  (is (= "../foo" (normalize-url "a/../../foo"))))

(deftest join-url-test
  (is (= "foo/bar" (join-url "foo" "bar")))
  (is (= "foo/bar" (join-url "foo" "bar")))
  (is (= "foo/bar" (join-url "foo/" "bar")))
  (is (= "foo/xxx" (join-url "foo/bar" "../xxx")))
  (is (= "foo bar/xxx" (join-url "foo bar" "xxx")))
  (is (= "foo%20bar/xxx" (join-url "foo%20bar" "xxx")))
  (is (= "a/d.less" (join-url "a/b/c" "../../d.less"))))

(def less
"@test: #fff;
 @import \"foo.less\";
 a { color: @test;}")

(def css
"span {
  color: #123;
}
h1 {
  font-size: 12px;
}
a {
  color: #fff;
}
")

(def test-folder (.toFile (Files/createTempDirectory "less4clj-input" (into-array FileAttribute []))))
(def test-file (io/file test-folder "test.main.less"))
(spit test-file less)

(deftest less-compile-test
  (testing "Compile file"
    (let [res (less-compile test-file {:source-map true})]
      (is (= (str css "/*# sourceMappingURL=test.css.map */\n") (:output res)))
      (is (= [] (:warnings res)))
      (println (:source-map res))
      (is (string/starts-with? (:source-map res) "{\n\"version\":3,\n\"file\":\"test.css\",\n\"lineCount\":7,\n"))))

  (testing "Compile string"
    (is (= {:output css :source-map nil :warnings []}
           (less-compile less {})))))

(deftest import-werbjars
  (is (less-compile "@import \"bootstrap/less/bootstrap.less\";" {})))

(def less-with-js
"@number: 100;
 @content: \"less symbol is < and more symbol is >\";
.logaritmic-thing {
  // escaped JavaScript - calculate logarithm
  margin: ~`Math.round(Math.log(@{number})*100)/100`;
  // embedded JavaScript - escape < and > characters
  content: `@{content}.replace(/</g, '&lt;').replace(/>/g, '&gt;')`;
}
")

(def css-with-js
".logaritmic-thing {
  margin: 4.61;
  content: \"less symbol is &lt; and more symbol is &gt;\";
}
")

(def test-file-with-js (File/createTempFile "less4clj" "test-js.less"))
(spit test-file-with-js less-with-js)

(deftest less-compile-test-with-js
  (is (= {:output css-with-js :source-map nil :warnings []}
         (less-compile test-file-with-js {:inline-javascript true})))

  (is (= {:output css-with-js :source-map nil :warnings []}
         (less-compile less-with-js {:inline-javascript true}))))

(deftest less-compile-error
  (is (thrown? clojure.lang.ExceptionInfo (less-compile "foosdfsdf%;" {})))

  (try
    (less-compile "foosdfsdf%;" {})
    (catch Exception e
      (let [{:keys [type errors]} (ex-data e)]
        (is (= :less4clj.core/error type))
        (is (= 2 (count errors)))))))

(def warning-file (doto (File/createTempFile "less4clj" "warning-test.less")
                    (spit "{ color: red }")))

(deftest less-compile-warning
  (is (= {:type "WARNING"
          :source {:name (.getName warning-file)
                   :uri (.toString (.toURI warning-file))}
          :line 1
          :char 1
          :message "Ruleset without selector encountered."}
         (first (:warnings (less-compile warning-file {}))))))
