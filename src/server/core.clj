(ns server.core
  (:require [ring.adapter.jetty :as ring-jetty]
            [reitit.ring :as ring]
            [ring.util.response :as r]
            [ring.middleware.params :as mw-params]
            [muuntaja.core :as m]
            [muuntaja.middleware :as mw]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [server.db :as db]))

(defn get-patients [req]
    (let [patients (db/get-patients {})]
    (-> patients
        (r/response)
        (assoc :headers {"Content-Type" "application/json"}))))

(def app
  (ring/ring-handler
   (ring/router
    ["/"
     ["api/"
      ["patients" {:get get-patients}]]
     ["" {:handler (fn [req] {:body "Create redirect screen" :status 200})}]]
    {:data {:muuntaja m/instance
            :middleware [mw-params/wrap-params muuntaja/format-middleware]}})))

(defn start []
  (ring-jetty/run-jetty #'app {:port  3001
                               :join? false}))



(comment
(def server (start))
(.stop server)
  )
