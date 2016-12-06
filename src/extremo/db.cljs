(ns extremo.db)

(def nodegit (js/require "nodegit"))
(def path (js/require "path"))

(def repo-path "./content/notes/.git")

(defn open-repo []
  (-> (.resolve path "./content/notes/.git")
      (nodegit.Repository.open)
      (.catch #(js/console.error "Something went wrong opening repo: " %))))

(defn get-entry-by-path [repo filepath]
  (-> repo
      (.then #(.getMasterCommit %))
      (.then #(.getEntry % filepath))))

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
                                     :date (.date (.-commit %))) commits)))
                   (.catch #(js/console.error %)))))))

(defn build-entry-data [entry]
  (let [rv (transient {})]
    (assoc! rv :name (.name entry) :sha (.sha entry))
    (-> (js/Promise.all [(.getBlob entry) (get-file-history (.path entry))])
        (.then (fn [[blob file-history]]
                 (persistent! (assoc! rv :rawsize (.rawsize blob)
                                         :blob (.toString blob)
                                         :updated_at (:date (first file-history))
                                         :created_at (:date (last file-history))
                                         :history file-history)))))))

(defn get-file-by-path [filepath]
  (let [rv (transient {})]
    (-> (open-repo)
        (.then #(.getMasterCommit %))
        (.then #(.getEntry % filepath))
        (.then #(build-entry-data %)))))

;; (-> (open-repo)
;;     (.then #(.getMasterCommit %))
;;     (.then #(.getTree %))
;;     (.then (fn [tree]
;;              (-> (.walk tree)
;;                  (.on "entry" #(js/console.log (.path %)))
;;                  (.start)))))

;; (-> (get-file-history "test.md")
;;     (.then #(js/console.log (clj->js %))))

(-> (get-file-by-path "test.md")
    (.then #(js/console.log (clj->js %))))
