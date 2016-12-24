(ns extremo.routes
  (:require
    [bidi.bidi :as bidi]
    [hiccups.runtime]
    [macchiato.util.response :as r]
    [extremo.db :as db])
  (:require-macros
    [hiccups.core :refer [html]]))

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

(defn process [items]
  (clj->js (mapv #(hash-map :name (:name %)) items)))

(defn notes [req res]
  (let [db-request (db/get-all-files)]
    (.then db-request #(-> (js/JSON.stringify (process %))
                           (r/ok)
                           (r/content-type "application/hal+json")
                           (res)))))

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
