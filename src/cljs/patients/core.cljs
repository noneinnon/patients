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

(def log (.-log js/console))
;; views
;; ----------------------------------------------------------------------------
(defn header []
  [:header
   [:h1 "Patients app"]
   [:nav
    [:a {:href (rfe/href ::patients-list)} "Patients list"]
    [:a {:href (rfe/href ::new)} "Create"]]])

(defn filter-panel []
  [:form {:on-submit (fn [e]
                       (.preventDefault e)
                       (let [form (new js/FormData (.-target e))
                             filter-name (.get form "filter-name")
                             filter-value (.get form "filter-value")]
                         (re-frame/dispatch [::events/fetch-patients {filter-name filter-value}])))}
   [:p "filter by:"]
   [:select {:name "filter-name"}
    [:option {:value "first_name"} "first name"]
    [:option {:value "last_name"} "last name"]
    [:option {:value "age"} "age"]
    [:option {:value "dob"} "date of birth"]
    [:option {:value "address"} "address"]
    [:option {:value "insurance_number"} "insurance number"]]
   [:input {:name "filter-value" :placeholder "enter value"}]
   [:button "filter"]])

(defn patient-row [{:keys [id first_name last_name age insurance_number dob address]}]
  [:tr {:key id :on-click (fn [e]
                            (let [target (.-target e)]
                              (if (= (.getAttribute target "name") "clickable")
                                (rfe/push-state ::patient {:id id})
                                0)))}
   [:td {:name "clickable"} first_name]
   [:td {:name "clickable"} last_name]
   [:td {:name "clickable"} age]
   [:td {:name "clickable"} dob]
   [:td {:name "clickable"} insurance_number]
   [:td {:name "clickable"} address]
   [:td {:name "controls"}
    [:button "✏️"]
    [:button "❌"]]])

(defn patients-list []
  (let [patients (re-frame/subscribe [::subs/patients])]
    (reagent/create-class
     {:component-did-mount (fn []
                             (re-frame/dispatch [::events/fetch-patients]))
      :reagent-render (fn []
                        [:section.container
                         [filter-panel]
                         (if (not-empty @patients)
                           [:table
                            [:thead
                             [:th "first name"]
                             [:th "last name"]
                             [:th "age"]
                             [:th "date of birth"]
                             [:th "insurance number"]
                             [:th "address"]]
                            [:tbody
                             (map patient-row @patients)]]
                           [:p "No patients found"])])})))

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
  [:div.content
   [header]
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
     {:use-fragment false})
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
