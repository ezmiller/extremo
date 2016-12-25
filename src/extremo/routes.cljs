(ns extremo.routes
  (:require
    [bidi.bidi :as bidi]
    [hiccups.runtime]
    [macchiato.util.response :as r]
    [extremo.db :as db]
    [clojure.walk :as walk]
    [clojure.pprint :as pp])
  (:require-macros
    [hiccups.core :refer [html]]))

(defn parse-int [s]
   (js/parseInt s 10))

(defn process-notes [notes url page]
  (def next-page (inc (if (nil? page) page 1)))
  (->> notes
       (mapv #(->> %
                   (:sha)
                   (str "/note/")
                   (hash-map :self)
                   (hash-map :_links)
                   (merge %)))
       (hash-map :notes)
       (hash-map :_embedded)
       (merge
         (hash-map :_links (hash-map :self (if (= page 1) "/notes" url)
                                     :next (str "/notes?page=" next-page))))))

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

(defn notes [req res]
  (let [db-request (db/get-all-files)
        url (:url req)
        curr-page (parse-int (:page (:params req)))]
    (js/console.log (:page (:params req)))
    (js/console.log curr-page)
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
   [["" home]
    ["notes" notes]
    [true not-found]]])

(defn router [req res raise]
  (let [route (->> req :uri (bidi/match-route routes) :handler)]
    (route req res raise)))
