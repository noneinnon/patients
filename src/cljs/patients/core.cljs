(ns patients.core
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reitit.frontend :as rf]
   [reitit.frontend.easy :as rfe]
   [reitit.coercion.spec :as rss]
   [re-frame.core :as re-frame]
   [patients.events :as events]
   [patients.subs :as subs]
   ["react-dom/client" :refer [createRoot]]))

;; views
;; ----------------------------------------------------------------------------
(defn patients-list []
  (let [patients (re-frame/subscribe [::subs/patients])
        state (re-frame/subscribe [::subs/state])]
    (reagent/create-class
     {:component-did-mount (fn []
                             (re-frame/dispatch [::events/fetch-patients]))
      :reagent-render (fn []
                        [:ul
                         [:h1 @state]
                         (if (not-empty @patients)
                           (for [patient @patients]
                             [:a {:key (:id patient)
                                  :href (rfe/href ::patient {:id (:id patient)})}
                              [:li.patient-container (:first_name patient) (:last_name patient) (:age patient)]])
                           [:li "No patients found"])])})))

(defn create-new-patient []
  [:form
   [:input {:type :text :name :first-name :placeholder "First name"}]
   [:input {:type :text :name :last-name :placeholder "Last name"}]])

(defn view-patient [match]
  (let [patient-id (get-in match [:path-params :id])
        patient (re-frame/subscribe [::subs/patient])
        state (re-frame/subscribe [::subs/state])]
    (reagent/create-class
     {:component-did-mount
      (fn []
        (re-frame/dispatch [::events/fetch-patient {:id patient-id}]))
      :reagent-render
      (fn []
        (case @state
          :loading [:section "Loading"]
          (if @patient
            [:section (:first_name @patient)]
            [:section "No patient with this ID exist"])))})))

;; routes
;; ----------------------------------------------------------------------------

(defonce match (atom nil))

(defn current-page []
  [:div
   [:ul
    [:li [:a {:href (rfe/href ::patients-list)} "Patients list"]]
    [:li [:a {:href (rfe/href ::new)} "Create"]]]
   (if @match
     ; (println str (:data @match))
     (let [view (:view (:data @match))]
       [view @match]))
   [:pre (with-out-str (str @match))]])

(def routes
  [["/"
    {:name ::patients-list
     :view patients-list}]

   ["/new"
    {:name ::new
     :view create-new-patient}]

   ["/patient/:id"
    {:name ::patient
     :view view-patient
     :parameters {:path {:id int?}}}]])

;; main
;; ----------------------------------------------------------------------------

(defn- main []
  (fn []
    (rfe/start!
     (rf/router routes {:data {:coercion rss/coercion}})
     (fn [m] (reset! match m))
          ;; set to false to enable HistoryAPI
     {:use-fragment true})
    [:main [current-page]]))

;; ----------------------------------------------------------------------------

(defonce root (createRoot (.getElementById js/document "app")))

(defn init
  []
  (re-frame/dispatch-sync [::events/initialize-db])
  (.render root (reagent/as-element [main])))

(defn ^:dev/after-load re-render
  []
  ;; The `:dev/after-load` metadata causes this function to be called
  ;; after shadow-cljs hot-reloads code.
  ;; This function is called implicitly by its annotation.
  (re-frame/clear-subscription-cache!)
  (init))
