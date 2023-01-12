(ns patients.core
  (:require [ring.adapter.jetty :as ring-jetty]
            [reitit.ring :as ring]
            [ring.util.response :as r]
            [ring.middleware.params :as mw-params]
            [muuntaja.core :as m]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [clojure.java.io :as io]
            [patients.db :as db]))

(defn home-page [_]
  (-> (slurp (io/resource "public/index.html"))
      (r/response)
      (assoc :status 200)))

(defn _patients [req]
    (let [patients (db/get-patients {})]
    (-> patients
        (r/response))))

(def app
  (ring/ring-handler
   (ring/router
    ["/"
     ["api/"
      ["patients" {:get _patients}]]
     ["assets/*" (ring/create-resource-handler {:root "public/assets"})]
     ["css/*" (ring/create-resource-handler {:root "public/css"})]
     ["" {:get home-page}]]
    {:data {:muuntaja m/instance
            :middleware [mw-params/wrap-params muuntaja/format-middleware]}})))

(defn start []
  (ring-jetty/run-jetty #'app {:port  3001
                               :join? false}))


(def server (start))
(comment
  (db/get-patients {})
  (get-patients {}))
