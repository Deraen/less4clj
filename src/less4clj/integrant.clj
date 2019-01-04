(ns less4clj.integrant
  (:require [less4clj.api :as api]
            [integrant.core :as ig]))

(defmethod ig/init-key ::less4clj [_ options]
  (api/start options))

(defmethod ig/halt-key! ::less4clj [this options]
  (api/stop this))

(defmethod ig/suspend-key! ::less4clj [this options]
  nil)

(defmethod ig/resume-key ::less4clj [key opts old-opts old-impl]
  (if (= opts old-opts)
    old-impl
    (do
      (ig/halt-key! key old-opts)
      (ig/init-key key opts))))
