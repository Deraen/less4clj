(ns less4clj.watcher
  (:require [watchtower.core :as wt]))

(defn start [source-paths f]
  (wt/watcher
     source-paths
     (wt/rate 100)
     (wt/file-filter wt/ignore-dotfiles)
     (wt/file-filter (wt/extensions :less))
     (wt/on-change f)))

(defn stop [watcher]
  (future-cancel watcher))
