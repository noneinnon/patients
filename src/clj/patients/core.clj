(ns patients.core
  (:require [ring.adapter.jetty :as ring-jetty]
            [reitit.ring :as ring]
            [ring.util.response :as r]
            [ring.middleware.params :as mw-params]
            [ring.logger :as logger]
            [muuntaja.core :as m]
            [patients.helpers :as helpers]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [bouncer.core :as b]
            [bouncer.validators :as v]
            [clojure.walk :refer [keywordize-keys]]
            [patients.db :as db]))

(defn get-path-param [req param-name]
  (-> (get-in req [:path-params param-name])
      (edn/read-string)))

;; middlewares
(defn errors?
  "Reply with 400 if errors, delegate to handler if ok"
  [errors handler request]
  (if ((complement nil?) errors)
    (-> errors
        (r/bad-request))
    (handler request)))

(defn validate-patient-data [handler]
  (fn [request]
    (let [[errors] (b/validate (:form-params request)
                               :first_name [v/required [v/matches #"[a-zA-Z]+"]]
                               :last_name [v/required v/string]
                               :age [v/required v/integer]
                              ; :dob [v/datetime "yyyy-MM-dd" v/required]
                               :insurance_number [v/required v/string])]
      (errors? errors handler request))))

(defn validate-update-patient-data [handler]
  (fn [request]
    (let [[errors] (b/validate (:form-params request)
                               :first_name [[v/matches #"[a-zA-Z]+"]]
                               :last_name v/string
                               :age v/number
                               ;:dob v/datetime
                               :insurance_number v/string)]
      (errors? errors handler request))))

(defn wrap-patient [handler]
  (fn [request]
    (let [patient-data (:form-params request)
          age (helpers/parse-int (:age patient-data))
          dob (helpers/string-to-date (:dob patient-data))]
      (handler (assoc request :form-params (assoc patient-data :age age :dob dob))))))

(defn wrap-params [handler]
  (fn [request]
    (handler (assoc request
                    :query-params (keywordize-keys (:query-params request))
                    :form-params (keywordize-keys (:form-params request))))))

(defn convert-params
  "Makes sure that selected paramteres value is present"
  [handler]
  (fn [request]
    (let [{:keys [limit offset age sort-param order]
           :or {sort-param :id order :desc limit 20 offset 0}} (:query-params request)]
      (handler (update request :query-params #(assoc %
                                                     :limit (helpers/parse-int limit)
                                                     :offset (helpers/parse-int offset)
                                                     :sort-param sort-param
                                                     :order order
                                                     :age (helpers/parse-int age)))))))

;; handlers
(defn home-page [_]
  (-> (slurp (io/resource "public/index.html"))
      (r/response)
      (r/header "content-type" "text/html")))

(defn get-all-patients [req]
  (let [params (:query-params req)
        patients (db/get-patients params)]
    (-> patients
        (r/response))))

(defn get-single-patient [req]
  (let [id (get-path-param req :id)
        patient (db/get-patients (assoc (:query-params req) :id id))]
    (if (not-empty patient)
      (-> patient
          (r/response))
      (-> "Patient with this id not found"
          (r/bad-request)))))

(defn create-patient [req]
  ;;https://github.com/ring-clojure/ring/blob/1.9.0/ring-core/src/ring/middleware/params.clj#L53
  (->> (db/create-patient (:form-params req))
       (r/created "/api/patients")))

(defn update-patient [req]
  (let [updates (:form-params req)
        id (get-path-param req :id)]
    (-> (db/update-patient id updates)
        (r/response))))

(defn delete-patient [req]
  (let [id (edn/read-string (get-in req [:path-params :id]))]
    (-> (db/delete-patient id)
        (r/response))))

;; app
; https://github.com/metosin/reitit/blob/master/doc/ring/ring.md#middleware
(def app
  (ring/ring-handler
   (ring/router
    ["/"
     ["api/"
      ["patients" {:middleware [wrap-params convert-params]}
       ["/:id" {:get get-single-patient :delete delete-patient :put {:handler update-patient :middleware [validate-update-patient-data]}}]
       ["" {:get get-all-patients :post {:middleware [wrap-patient validate-patient-data] :handler create-patient}}]]]
     ["assets/*" (ring/create-resource-handler {:root "public/assets"})]
     ["css/*" (ring/create-resource-handler {:root "public/css"})]
     ["js/*" (ring/create-resource-handler {:root "public/js"})]
     ["" {:get home-page}]]
    {:data {:muuntaja m/instance
            :middleware [mw-params/wrap-params muuntaja/format-middleware]}})))

(defn start []
  (ring-jetty/run-jetty (logger/wrap-with-logger app) {:port 3001
                                                       :join? false}))

(comment
  (def server (start))
  (.stop server))
