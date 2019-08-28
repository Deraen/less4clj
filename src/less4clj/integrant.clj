(ns less4clj.integrant
  (:require [less4clj.api :as api]
            [integrant.core :as ig]))

(defmethod ig/init-key ::less4clj [_ options]
  (api/start options))

(defmethod ig/halt-key! ::less4clj [_ watcher]
  (api/stop watcher))

(defmethod ig/suspend-key! ::less4clj [_ watcher]
  nil)

(defmethod ig/resume-key ::less4clj [key opts old-opts old-impl]
  (if (= opts old-opts)
    old-impl
    (do
      (ig/halt-key! key old-impl)
      (ig/init-key key opts))))
