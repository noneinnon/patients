(ns patients.events
  (:require
   [re-frame.core :as re-frame]
   [patients.db :as db]
   [ajax.core :as ajax]
   [reitit.frontend.controllers :as rfc]
   [day8.re-frame.http-fx]
   [reitit.frontend.easy :as rfe]))

(defn create-notification [level message]
  {:id (new js/Date) :level level :message message})

(defn get-error-msg [data default-msg]
  (let [response (:response data)]
    (if response
      (str (vals response))
      default-msg)))

(re-frame/reg-event-db
 ::initialize-db
 (fn []
   db/default-db))

(re-frame/reg-event-db
 ::set-match
 (fn [db [_ new-match]]
   (assoc db :match
          (assoc new-match :controllers
                 (rfc/apply-controllers (get-in db [:match :data :controllers]) new-match)))))

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
                 :on-success [::on-create-success]
                 :on-failure [::on-create-failure]}

    :db (assoc db :state :loading)}))

(re-frame/reg-event-fx
 ::on-create-success
 (fn [_ _]
   {:fx [[:dispatch [::move-to-patients-list]]
         [:dispatch [::notify (create-notification :info "successfully created!")]]]}))

(re-frame/reg-event-fx
 ::on-create-failure
 (fn [_ [_ data]]
   {:fx [[:dispatch [::notify (create-notification :error (get-error-msg data "failed to create patient"))]]]}))

(re-frame/reg-event-fx
 ::update-patient
 (fn [{db :db} [_ params]]
   {:http-xhrio {:method :put
                 :uri (str "/api/patients/" (:id params))
                 :params params
                 :format          (ajax/url-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [::on-update-success]
                 :on-failure [::on-update-failure]}}))

(re-frame/reg-event-fx
 ::on-update-success
 (fn [_ _]
   {:fx [[:dispatch [::move-to-patients-list]]
         [:dispatch [::notify (create-notification :info "successfully updated!")]]]}))

(re-frame/reg-event-fx
 ::on-update-failure
 (fn [_ [_ data]]
   {:fx [[:dispatch [::notify (create-notification :error (get-error-msg data "failed to update patient"))]]]}))

(re-frame/reg-event-fx
 ::delete-patient
 (fn [{db :db} [_ {:keys [id]}]]
   {:http-xhrio {:method :delete
                 :uri (str "/api/patients/" id)
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [::on-delete-success]
                 :on-failure [::on-delete-failure]}}))

(re-frame/reg-event-fx
 ::on-delete-success
 (fn [_ _]
   {:fx [[:dispatch [::fetch-patients]]
         [:dispatch [::notify (create-notification :info "successfully deleted!")]]]}))

(re-frame/reg-event-fx
 ::on-delete-failure
 (fn [_ _]
   {:fx [[:dispatch [::notify (create-notification :error "there was an error deleting patient")]]]}))

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

(re-frame/reg-event-fx
 ::notify
 (fn [{:keys [db]} [_ data]]
   {:db (update db :notifications #(assoc % (:id data) data))
    :dispatch-later {:ms 5000 :dispatch [::remove-notification data]}}))

(re-frame/reg-event-db
 ::remove-notification
 (fn [db [_ data]]
   (-> db
       (update :notifications #(dissoc % (:id data))))))

(comment
  (.alert js/window "hey")
  (re-frame/dispatch [::notify {:id 1 :level :info :message "hello"}]))
