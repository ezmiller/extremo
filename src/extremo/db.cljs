(ns extremo.db)

(def nodegit (js/require "nodegit"))
(def path (js/require "path"))

(def repo-path "./content/notes/.git")

(defn open-repo []
  (-> (.resolve path "./content/notes/.git")
      (nodegit.Repository.open)
      (.catch #(js/console.error "Something went wrong opening repo: " %))))

(defn get-file-by-path [filepath]
  (let [rv (transient {})]
    (-> (open-repo)
        (.then #(.getMasterCommit % "d909ade91aae7970a34ba91e800493f9bac7d473"))
        (.then #(do (assoc! rv :date (.date %))
                    (.getEntry % filepath)))
        (.then #(do (assoc! rv :name (.name %) :sha (.sha %))
                    (.getBlob %)))
        (.then #(persistent! ( assoc! rv :rawsize (.rawsize %)
                                         :blob (.toString %)))))))

(-> (get-file-by-path "test.md")
    (.then #(js/console.log (clj->js %)))
    (.catch #(js/console.error %)))

;; (-> (open-repo)
;;     (.then #(.getMasterCommit %))
;;     (.then #(.getTree %))
;;     (.then (fn [tree]
;;              (-> (.walk tree)
;;                  (.on "entry" #(js/console.log (.path %)))
;;                  (.start)))))
