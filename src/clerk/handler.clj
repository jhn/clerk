(ns clerk.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.util.response :refer [response header]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [clj-time.core :as t]
            [clj-time.format :as f])
  (:gen-class))

(def db
  {:apple  {:campaigns [{:name  "watch"
                         :start (t/date-time 2015 1 1)
                         :end   (t/date-time 2015 12 31)
                         :creatives 5}
                        {:name  "car"
                         :start (t/date-time 2015 6 1)
                         :end   (t/date-time 2016 6 10)
                         :creatives 5}]}
   :bieber {:campaigns [{:name  "sorry"
                         :start (t/date-time 2016 5 1)
                         :end   (t/date-time 2016 5 30)
                         :creatives 5}
                        {:name "tour"
                         :start (t/date-time 2016 5 7)
                         :end   (t/date-time 2016 6 30)
                         :creatives 5}]}})

(defn creatives [db account-name campaign-name date]
  (when-let [account (db (keyword account-name))]
    (when-let [campaign (->> (account :campaigns)
                             (filter #(= campaign-name (:name %)))
                             (first))]
      (when (t/within? (t/interval (:start campaign) (:end campaign))
                       date)
        (campaign :creatives)))))

(defn gen-stats [account campaign date id]
  (let [url       (str "http://pulsepoint.com?account=" account "&campaign=" campaign "&id=" id)
        pageviews (quot (Math/abs ^int (.hashCode (str date (repeat id id)))) 10000)
        clicks    (quot pageviews (+ id 99))]
    {:url       url
     :pageviews pageviews
     :clicks    clicks
     :date      date}))

(defn stats [account campaign date]
  (try
    (let [parsed-date (f/parse (f/formatters :year-month-day) date)]
      (if-let [n (creatives db account campaign parsed-date)]
        (map #(gen-stats account campaign date %)
             (range 1 (+ n 1)))
        '()))
    (catch Exception e '()))) ; return error res instead

(defroutes app-routes
  (POST "/stats" [account campaign date] (header (response (stats account campaign date)) "Content-Type" "application/json"))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-defaults api-defaults)
      (wrap-json-params)
      (wrap-json-response)))

(defn -main [& args]
  (run-jetty app {:port 3000}))
