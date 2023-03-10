(ns patients.routes
  (:require
   [patients.events :as events]
   [patients.helpers :as helpers]
   [patients.subs :as subs]
   [patients.views :as views]
   [re-frame.core :as re-frame]
   [reitit.coercion.spec :as rss]
   [reitit.frontend :as rf]
   [reitit.frontend.easy :as rfe]))

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
                             (let [limit (:limit query)
                                   offset (:offset query)]
                               (if (or (not limit)
                                       (not offset))
                                 (rfe/replace-state :patients-list {}
                                                    (cond-> query
                                                      (not limit) (assoc :limit 20)
                                                      (not offset) (assoc :offset 0)))
                                 (re-frame/dispatch [::events/fetch-patients query]))))}]}]

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
