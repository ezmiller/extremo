(ns extremo.db)

(def nodegit (js/require "nodegit"))
(def path (js/require "path"))

(def repo-path "./content/notes/.git")

(defn open-repo
  "Returns a promise for the repo specified by repo-path."
  []
  (-> (.resolve path repo-path)
      (nodegit.Repository.open)
      (.catch #(js/console.error "Something went wrong opening repo: " %))))

(defn get-entry-by-sha
  "Returns a promised value for the entry specified by the sha."
  [repo sha]
  (-> repo
      (.then #(.getMasterCommit %))
      (.then #(.getTree %))
      (.then #(.entries %))
      (.then (fn [entries]
               (filterv #(= (.sha %) sha) entries)))
      (.then #(if (empty? %) nil (first %)))))

(defn get-entry-by-path
  "Returns a promise for the entry specified by filepath in repo."
  [repo filepath]
  (-> repo
      (.then #(.getMasterCommit %))
      (.then #(.getEntry % filepath))))

(defn get-file-history
  "Returns a promise for hash-map containing information data about
   file specified by filepath."
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

(defn get-file-by-sha [sha]
  (-> (open-repo)
      (get-entry-by-sha sha)
      (.then #(build-entry-data %))))

(defn get-file-by-path [filepath]
  (let [rv (transient {})]
    (-> (open-repo)
        (get-entry-by-path filepath)
        (.then #(build-entry-data %)))))

(defn get-all-files
  "Returns a promise vector for each file in the repo."
  []
  (let [rv (transient [])]
    (-> (open-repo)
        (.then #(.getMasterCommit %))
        (.then #(.getTree %))
        (.then (fn [tree]
                 (-> (.walk tree)
                     (.on "entry" #(conj! rv (build-entry-data %)))
                     (.start))))
        (.then #(js/Promise.all (persistent! rv))))))

;; (-> (get-all-files)
;;     (.then #(js/console.log %)))

;; (-> (get-file-by-path "test.md")
;;     (.then #(js/console.log (clj->js %))))

;; (-> (get-file-by-sha "96a357d8e67de8d4f3992cf4255019bbcae6da08")
;;     (.then #(js/console.log (clj->js %))))

;; (-> (open-repo)
;;     (get-entry-by-sha "96a357d8e67de8d4f3992cf4255019bbcae6da08")
;;     (.then #(build-entry-data %))
;;     (.then #(js/console.log (clj->js %)))
;;     (.catch #(js/console.error %)))
