(ns core
  (:require
   [cljs.core.async :as async :refer [go <!]]
   [reagent.core :as reagent :refer [atom]]
   [reitit.frontend :as rf]
   [reitit.frontend.easy :as rfe]
   [reitit.coercion.spec :as rss]
   [cljs-http.client :as http]
   ["react-dom/client" :refer [createRoot]]))

;; views
;; ----------------------------------------------------------------------------
(defn patients-list [match]
  (let [patients (get-in match [:data :state :data])]
    [:ul
     (if (not-empty patients)
       (for [patient patients]
         [:a {:key (get patient :id) 
              :href (rfe/href ::patient {:id (:id patient)})}
          [:li.patient-container (:first_name patient) (:last_name patient)]])
       [:li "no patients found"])]))

(defn create-new-patient []
  [:form
   [:input {:type :text :name :first-name :placeholder "First name"}]
   [:input {:type :text :name :last-name :placeholder "Last name"}]])

(defn view-patient [patient]
  [:section (:age patient)])

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
  (let [state (atom {:data nil :state "init"})] ;; you can include state
    (reagent/create-class
     {:component-did-mount
      (fn []
        (go (let [response (<! (http/get "/api/patients"))
                  data (:body response)]
              (swap! state assoc :data data :state "done"))))

       ;; ... other methods go here

       ;; name your component for inclusion in error messages
      :display-name "complex-component"

       ;; note the keyword for this method
      :reagent-render
      (fn []
        (rfe/start!
         (rf/router routes {:data {:coercion rss/coercion :state @state}})
         (fn [m] (reset! match m))
          ;; set to false to enable HistoryAPI
         {:use-fragment true})
        [:main [current-page]])})))

;; ----------------------------------------------------------------------------

(defonce root (createRoot (.getElementById js/document "app")))

(defn init
  []
  (.render root (reagent/as-element [main])))

(defn ^:dev/after-load re-render
  []
  ;; The `:dev/after-load` metadata causes this function to be called
  ;; after shadow-cljs hot-reloads code.
  ;; This function is called implicitly by its annotation.
  (init))
