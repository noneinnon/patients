(ns patients.core
  (:require [ring.adapter.jetty :as ring-jetty]
            [reitit.ring :as ring]
            [ring.util.response :as r]
            [ring.middleware.params :as mw-params]
            [muuntaja.core :as m]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [patients.db :as db]))

(defn home-page [_]
  (-> (slurp (io/resource "public/index.html"))
      (r/response)
      (assoc :status 200)))

(defn get-all-patients [req]
  (let [patients (db/get-patients {})]
    (-> patients
        (r/response)
        (assoc :status 200))))

(defn get-single-patient [req]
  (let [id (edn/read-string (get-in req [:path-params :id]))
        patient (first (db/get-patients {:where [:= :patients.id id]}))]
    (-> patient
        (r/response)
        (assoc :status 200))))

(def app
  (ring/ring-handler
   (ring/router
    ["/"
     ["api/"
      ["patients"
       ["/:id" {:get get-single-patient}]
       ["" {:get get-all-patients}]]]
     ["assets/*" (ring/create-resource-handler {:root "public/assets"})]
     ["css/*" (ring/create-resource-handler {:root "public/css"})]
     ["js/*" (ring/create-resource-handler {:root "public/js"})]
     ["" {:get home-page}]]
    {:data {:muuntaja m/instance
            :middleware [mw-params/wrap-params muuntaja/format-middleware]}})))

(defn start []
  (ring-jetty/run-jetty #'app {:port  3001
                               :join? false}))

(def server (start))
(comment
  (.stop server)
  (db/get-patients {:where [:= :patients.id 1]}))
