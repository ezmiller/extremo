(ns extremo.middleware
  (:require
    [macchiato.defaults :as defaults]))

(defn wrap-defaults [handler]
  (-> handler
      ;; can add other middlewweare here possibly.
      (defaults/wrap-defaults defaults/site-defaults)))


