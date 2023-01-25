(ns patients.core
  (:require [ring.adapter.jetty :as ring-jetty]
            [reitit.ring :as ring]
            [ring.util.response :as r]
            [ring.middleware.params :as mw-params]
            [muuntaja.core :as m]
            [patients.helpers :as helpers]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [bouncer.core :as b]
            [bouncer.validators :as v]
            [clojure.walk :refer [keywordize-keys]]
            [patients.db :as db]))

;; validation
(defn validate-patient-data [patient]
  (b/validate patient
              :first_name [v/required v/string]
              :last_name [v/required v/string]
              :age [v/required v/integer]
              ; :dob [v/datetime "yyyy-MM-dd" v/required]
              :insurance_number [v/required v/string]))

(defn validate-update-patient-data [updates]
  (b/validate updates
              :first_name v/string
              :last_name v/string
              :age v/number
              ; :dob v/datetime
              :insurance_number v/string))

(defn get-path-param [req param-name]
  (-> (get-in req [:path-params param-name])
      (edn/read-string)))
;; handlers
(defn home-page [_]
  (-> (slurp (io/resource "public/index.html"))
      (r/response)
      (r/header "content-type" "text/html")))

(defn get-all-patients [req]
  (let [patients (db/get-patients {})]
    (-> patients
        (r/response))))

(defn get-single-patient [req]
  (let [id (get-path-param req :id)
        patient (db/get-patients {:where [:= :patients.id id]})]
    (if (not-empty patient)
      (-> patient
          (r/response))
      (-> "Patient with this id not found"
          (r/bad-request)))))

(defn wrap-patient [handler]
  (fn [request]
    (let [patient-data (keywordize-keys (:form-params request))
          age (helpers/parse-int (:age patient-data))
          dob (helpers/string-to-date (:dob patient-data))]
      (handler (assoc request :form-params (assoc patient-data :age age :dob dob))))))

(defn create-patient [req]
  ;;https://github.com/ring-clojure/ring/blob/1.9.0/ring-core/src/ring/middleware/params.clj#L53
  (let [[errors] (validate-patient-data (:form-params req))]
    (if (nil? errors)
    (->> (db/create-patient (:form-params req))
         (r/created "/api/patients"))
    (-> errors
        (r/bad-request)))))

(defn update-patient [req]
  (let [updates (:form-params req)
        id (get-path-param req :id)
        [errors] (validate-update-patient-data updates)]
    (if (nil? errors)
      (-> (db/update-patient id updates)
          (r/response))
      (-> errors
          (r/bad-request)))))

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
      ["patients"
       ["/:id" {:get get-single-patient :delete delete-patient :put update-patient}]
       ["" {:get get-all-patients :post {:middleware [wrap-patient] :handler create-patient}}]]]
     ["assets/*" (ring/create-resource-handler {:root "public/assets"})]
     ["css/*" (ring/create-resource-handler {:root "public/css"})]
     ["js/*" (ring/create-resource-handler {:root "public/js"})]
     ["" {:get home-page}]]
    {:data {:muuntaja m/instance
            :middleware [mw-params/wrap-params muuntaja/format-middleware]}})))

(defn start []
  (ring-jetty/run-jetty #'app {:port  3001
                               :join? false}))

; (def server (start))

(comment
  (.stop server))
