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

(defn get-file-history
  [filepath]
  ;; TODO: At the momment this will only fetch 50 commits into the
  ;;       file history. It may be that the max that fileHisotryWalk
  ;;       can fetch is 500 at a time. The sample code for this kind
  ;;       of walk in the nodegit repo, shows some code that seems to
  ;;       compile the code 500 commits at a time.
  (-> (open-repo)
      (.then (fn [repo]
               (-> (.getMasterCommit repo)
                   (.then #(let [commit %
                                 walker (.createRevWalk repo)]
                             (.push walker (.sha commit))
                             (.sorting walker nodegit.Revwalk.SORT.Time)
                             (.fileHistoryWalk walker filepath 50)))
                   (.then (fn [commits]
                            (mapv #(assoc
                                     {}
                                     :sha (.sha (.-commit %))
                                     :date (.date (.-commit %))
                                     :commit (.-commit %)) commits)))
                   (.catch #(js/console.error %)))))))

;; (-> (open-repo)
;;     (.then #(.getMasterCommit %))
;;     (.then #(.getTree %))
;;     (.then (fn [tree]
;;              (-> (.walk tree)
;;                  (.on "entry" #(js/console.log (.path %)))
;;                  (.start)))))
