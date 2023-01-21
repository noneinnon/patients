(ns patients.events
  (:require
   [re-frame.core :as re-frame]
   [patients.db :as db]
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   ; [ajax.core :as ajax]
   ; [day8.re-frame.tracing :refer-macros [fn-traced]]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn []
   db/default-db))

(re-frame/reg-event-fx        ;; <-- note the `-fx` extension
 ::fetch-patients        ;; <-- the event id
 (fn                ;; <-- the handler function
   [{db :db} [_ params]]     ;; <-- 1st argument is coeffect, from which we extract db 
    ;; we return a map of (side) effects
   {:http-xhrio {:method          :get
                 :uri             "/api/patients"
                 :params  params
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [::fetch-patients-success]
                 :on-failure      [::fetch-patients-failure]}
    :db  (assoc db :state :loading)}))

(re-frame/reg-event-db
 ::fetch-patients-success
 (fn [db [_ data]]
   (-> db
       (assoc :state :success)
       (assoc :patients data))))

(re-frame/reg-event-db
 ::fetch-patients-failure
 (fn [db [_ {:keys [data]}]]
   (-> db
       (assoc :state :failure)
       (assoc :error data))))

(re-frame/reg-event-fx
 ::fetch-patient
 (fn
   [{db :db} [_ {:keys [id]}]]
   {:http-xhrio {:method          :get
                 :uri             (str "/api/patients/" id)
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [::fetch-patient-success]
                 :on-failure      [::fetch-patient-failure]}
    :db  (assoc db :state :loading)}))

(re-frame/reg-event-db
 ::fetch-patient-success
 (fn [db [_ data]]
   (-> db
       (assoc :state :success)
       (assoc :patient data))))

(re-frame/reg-event-db
 ::fetch-patient-failure
 (fn [db [_ {:keys [data]}]]
   (-> db
       (assoc :state :failure)
       (assoc :error data))))
