(ns extremo.routes
  (:require
    [bidi.bidi :as bidi]
    [hiccups.runtime]
    [macchiato.util.response :as r]
    [extremo.db :as db]
    [clojure.walk :as walk]
    [clojure.pprint :as pp]
    [clojure.string :as str])
  (:require-macros
    [hiccups.core :refer [html]]))

(defn parse-int [s]
   (js/parseInt s 10))

(defn process-note [note]
  (->> note
       (:sha)
       (str "/api/note/")
       (hash-map :self)
       (hash-map :_links)
       (merge note)))

(defn process-notes [notes url page]
  (def next-page (inc (if (nil? page) page 1)))
  (->> notes
       (mapv #(->> %
                   (:sha)
                   (str "/api/note/")
                   (hash-map :self)
                   (hash-map :_links)
                   (merge %)))
       (hash-map :notes)
       (hash-map :_embedded)
       (merge
         (hash-map :_links (hash-map :self (if (= page 1) "/api/notes" url)
                                     :next (str "/api/notes?page=" next-page))))))

(defn home [req res raise]
  (-> (html
        [:html
         [:body
          [:h2 "Hello World!"]
          [:p
           "Your user-agent is: "
           (str (get-in req [:headers "user-agent"]))]]])
      (r/ok)
      (r/content-type "text/html")
      (res)))

(defn get-note [req res raise]
  (let [uri (:uri req)
        note-sha (nth (str/split uri #"/") 3)
        db-request (db/get-file-by-sha note-sha)]
    (.then db-request (fn [note]
      (-> (process-note note)
          (clj->js)
          (js/JSON.stringify)
          (r/ok)
          (r/content-type "application/hal+json")
          (res))))))

(defn get-notes [req res]
  (let [db-request (db/get-all-files)
        url (:url req)
        curr-page (parse-int (:page (:params req)))]
    (.then db-request (fn [notes]
                        (-> (process-notes notes url curr-page)
                            (clj->js)
                            (js/JSON.stringify)
                            (r/ok)
                            (r/content-type "application/hal+json")
                            (res))))))

(defn not-found [req res raise]
  (-> (html
        [:html
         [:body
          [:h2 (:uri req) " was not found"]]])
      (r/not-found)
      (r/content-type "text/html")
      (res)))

(def routes
  ["/"
   [["home" home]
    ["api/" {"note/"  {[:sha ""] {:get get-note}}
             "notes" {"" {:get get-notes}}}]
    [true not-found]]])

;; (def routes
;;   ["/" {"home" home
;;         "notes" {"" {:get get-notes}}
;;         "note/" {[:sha ""] {:get get-note}}}])

(defn router [req res raise]
  (let [uri (:uri req)
        method (:request-method req)
        route (->> (bidi/match-route routes uri :request-method method) :handler)]
    (route req res raise)))
