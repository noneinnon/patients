(ns patients.routes
  (:require
   [reitit.frontend :as rf]
   [reitit.frontend.easy :as rfe]
   [reitit.coercion.spec :as rss]
   [patients.views :as views]
   [patients.events :as events]
   [re-frame.core :as re-frame]
   [patients.subs :as subs]))

(defn current-page []
  (let [match @(re-frame/subscribe [::subs/match])]
    [:div.content
     [views/header]
     (if match
       (let [view (:view (:data match))]
         [view match])
       [:pre (with-out-str (str match))])]))

(def routes
  [["/"
    {:name :patients-list
     :view views/patients-list
     :controllers [{:parameters {:query [:limit :offset :sort-param :order :first_name :last_name :age :insurance_number]}
                    :start (fn [{:keys [query]}]
                             (re-frame/dispatch [::events/fetch-patients query]))}]}]

   ["/new"
    {:name :new
     :view views/create-new-patient}]

   ["/patient/:id"
    {:name :patient
     :view views/patient-page
     :parameters {:path {:id int?}}
     :controllers [{:parameters {:path [:id]}
                    :start (fn [{:keys [path]}]
                             (re-frame/dispatch [::events/fetch-patient {:id (:id path)}]))}]}]])

(defn start-router []
  (rfe/start!
   (rf/router routes {:data {:coercion rss/coercion}})
   (fn [new-match]
     (re-frame/dispatch [::events/set-match new-match]))
     ; (fn [m] (reset! match m))
          ;; set to false to enable HistoryAPI
   {:use-fragment true}))
