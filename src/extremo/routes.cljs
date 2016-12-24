(ns extremo.routes
  (:require
    [extremo.db]
    [bidi.bidi :as bidi]
    [hiccups.runtime]
    [macchiato.response :as r]
    [extremo.db :as db])
  (:require-macros
    [hiccups.core :refer [html]]))

(defn home [req res]
  (-> (html
        [:html
         [:body
          [:h2 "Hello World!"]
          [:p
           "Your user-agent is: "
           (str (-> req :headers :user-agent))]]])
      (r/ok)
      (res)))

(defn process [items]
  (clj->js (mapv #(hash-map :name (:name %)) items)))

(defn notes [req res]
  (let [db-request (db/get-all-files)]
    (.then db-request #(res (r/content-type
                              (r/ok (js/JSON.stringify (process %)))
                              "application/json")))))

(defn not-found [req res]
  (-> (html
        [:html
         [:body
          [:h2 (:uri req) " was not found"]]])
      (r/not-found)
      (res)))

(def routes
  ["/"
   [["" home]
    ["notes" notes]
    [true not-found]]])

(defn router [req res]
  (let [route (->> req :uri (bidi/match-route routes) :handler)]
    (route req res)))
