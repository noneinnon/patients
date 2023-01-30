(ns patients.core
  (:require [ring.adapter.jetty :as ring-jetty]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as rrc]
            [reitit.coercion.spec]
            [reitit.dev.pretty :as pretty]
            [ring.util.response :as r]
            [ring.middleware.params :as mw-params]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [muuntaja.core :as m]
            [patients.helpers :as helpers]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [bouncer.core :as b]
            [bouncer.validators :as v]
            [patients.db :as db])
  (:gen-class))

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

(defn min? [value minimum]
  (if value
    (> value minimum)
    false))

(defn date? [value]
  (if value (try (java.time.LocalDate/parse value)
                 (catch Exception _ false))
      true))

(defn validate-patient-data [handler]
  (fn [request]
    (let [[errors] (b/validate (:params request)
                               :first_name [v/required [v/matches #"[a-zA-Z]+"]]
                               :last_name [v/required [v/matches #"[a-zA-Z]+"]]
                               :age [v/required v/number [min? 1 :message "Age should be higher than 1"]]
                               :sex [v/required [v/matches #"m|f"]]
                               :dob [v/required [date? :message "Date not valid"]]
                               :insurance_number [v/required [v/matches #"\d+"]])]
      (errors? errors handler request))))

(defn validate-update-patient-data [handler]
  (fn [request]
    (let [[errors] (b/validate (:params request)
                               :first_name [[v/matches #"[a-zA-Z]+"]]
                               :last_name [[v/matches #"[a-zA-Z]+"]]
                               :age [v/number [min? 1 :message "Age should be higher than 1"]]
                               :sex [[v/matches #"m|f"]]
                               :dob [[date? :message "Date not valid"]]
                               :insurance_number [[v/matches #"\d+"]])]
      (errors? errors handler request))))

(defn convert-form-params [handler]
  (fn [request]
    (let [{:keys [age dob]} (:params request)]
      (handler (update request :params #(cond-> %
                                          dob (assoc :dob (helpers/string-to-date dob))
                                          age (assoc :age (helpers/parse-int age))))))))

(defn logger
  "simple logging middleware"
  [handler]
  (fn [request]
    (prn (select-keys request [:request-method :uri :params :form-params]))
    (handler request)))

(defn convert-params
  "Makes sure that selected paramteres value is present"
  [handler]
  (fn [request]
    (let [{:keys [limit offset sort-param order age]
           :or {sort-param :id order :desc limit 20 offset 0}} (:params request)]
      (handler (update request :params #(assoc %
                                               :limit (helpers/parse-int limit)
                                               :offset (helpers/parse-int offset)
                                               :sort-param (keyword sort-param)
                                               :order (keyword order)
                                               ; :dob (helpers/string-to-date dob)
                                               :age (helpers/parse-int age)))))))

;; handlers
(defn home-page [_]
  (-> (slurp (io/resource "public/index.html"))
      (r/response)
      (r/header "content-type" "text/html")))

(defn get-all-patients [req]
  (let [params (:params req)
        patients (db/get-patients params)]
    (-> patients
        (r/response))))

(defn get-single-patient [req]
  (let [id (get-path-param req :id)
        [patient] (db/get-patients (assoc (:params req) :id id))]
    (if (not-empty patient)
      (-> patient
          (r/response))
      (-> "Patient with this id not found"
          (r/bad-request)))))

(defn create-patient [req]
  ;;https://github.com/ring-clojure/ring/blob/1.9.0/ring-core/src/ring/middleware/params.clj#L53
  (->> (db/create-patient (:params req))
       (r/created "/api/patients")))

(defn update-patient [req]
  (let [updates (:params req)
        id (get-path-param req :id)]
    (-> (db/update-patient id updates)
        (r/response))))

(defn delete-patient [req]
  (let [id (edn/read-string (get-in req [:path-params :id]))]
    (-> (db/delete-patient id)
        (r/response))))

;; app
(def muuntaja-instance
  (m/create
   (assoc-in
    m/default-options
    [:formats "application/json" :encoder-opts]
    {:date-format "yyyy-MM-dd"})))

; https://github.com/metosin/reitit/blob/master/doc/ring/ring.md#middleware
(def app
  (ring/ring-handler
   (ring/router
    ["/"
     ["api/"
      ["patients" {:middleware [convert-params]}
       ["/:id" {:get get-single-patient
                :delete delete-patient
                :put {:handler update-patient :middleware [validate-update-patient-data convert-form-params]}}]
       ["" {:get get-all-patients
            :post {:middleware [validate-patient-data convert-form-params] :handler create-patient}}]]]
     ["assets/*" (ring/create-resource-handler {:root "public/assets"})]
     ["css/*" (ring/create-resource-handler {:root "public/css"})]
     ["js/*" (ring/create-resource-handler {:root "public/assets/js"})]
     ["" {:get home-page}]]


    {:exception pretty/exception
     :data {:muuntaja muuntaja-instance
            :middleware [rrc/coerce-exceptions-middleware
                         rrc/coerce-request-middleware
                         rrc/coerce-response-middleware
                         muuntaja/format-request-middleware
                         muuntaja/format-response-middleware
                         muuntaja/format-middleware
                         mw-params/wrap-params
                         wrap-keyword-params
                         logger]}})))

(defn start []
  (ring-jetty/run-jetty #'app {:port 3001
                             :join? false}))

(defn -main []
  (prn "starting server")
  (start))

(comment
  (app {:request-method :get :uri "/api/patients/665" :headers {}})
  (def server (start))
  (.stop server))
