(ns patients.events
  (:require
   [re-frame.core :as re-frame]
   [patients.db :as db]
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   [reitit.frontend.easy :as rfe]))

(re-frame/reg-event-db
 ::initialize-db
 (fn []
   db/default-db))

(re-frame/reg-event-fx        ;; <-- note the `-fx` extension
 ::fetch-patients        ;; <-- the vent id
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

(re-frame/reg-event-fx
 ::create-patient
 (fn [{db :db} [_ params]]
   {:http-xhrio {:method :post
                 :uri "/api/patients"
                 :params params
                 :format          (ajax/url-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [::move-to-patients-list]}
    :db (assoc db :state :loading)}))

(re-frame/reg-event-fx
 ::update-patient
 (fn [{db :db} [_ params]]
   (prn params)
   {:http-xhrio {:method :put
                 :uri (str "/api/patients/" (:id params))
                 :params params
                 :format          (ajax/url-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [::move-to-patients-list]
                 :on-failure [::move-to-patients-list]}
    :db (assoc db :state :loading)}))

(re-frame/reg-event-fx
 ::delete-patient
 (fn [{db :db} [_ {:keys [id]}]]
   {:http-xhrio {:method :delete
                 :uri (str "/api/patients/" id)
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [::fetch-patients]}
    :db (assoc db :state :loading)}))

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

;; success fx
(re-frame/reg-event-fx
 ::move-to-patients-list
 (fn []
   (rfe/push-state :patients-list)))

(re-frame/reg-event-db
 ::fetch-patients-success
 (fn [db [_ [patients total]]]
   (-> db
       (assoc :state :success :patients patients :total total))))

(re-frame/reg-event-db
 ::fetch-patient-success
 (fn [db [_ [patient]]]
   (-> db
       (assoc :state :success :patient patient))))

;; failure fx
(re-frame/reg-event-db
 ::fetch-patients-failure
 (fn [db [_ {:keys [data]}]]
   (-> db
       (assoc :state :failure :error data))))

(re-frame/reg-event-db
 ::fetch-patient-failure
 (fn [db [_ {:keys [data]}]]
   (-> db
       (assoc :state :failure :error data))))
