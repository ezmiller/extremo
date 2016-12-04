(ns extremo.db)

(def nodegit (js/require "nodegit"))
(def path (js/require "path"))

(def repo-path "./content/notes/.git")

(defn build-fild-data [file blob])

(defn open-repo []
  (-> (.resolve path "./content/notes/.git")
      (nodegit.Repository.open)
      (.catch #(js/console.error "Something went wrong opening repo: " %))))

(defn get-file-by-path [filepath]
  (-> (open-repo)
      (.then #(.getCommit % "d909ade91aae7970a34ba91e800493f9bac7d473"))
      (.then #(js/Promise.all [(.getEntry % filepath) {:date (.date %)}]))
      (.then #(js/Promise.all
                [(.getBlob (first %))
                 (assoc (second %) :name (.name (first %)) :sha (.sha (first %)))]))
      (.then #(assoc (second %) :rawsize (.rawsize (first %))
                                 :blob (.toString (first %))))))

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
