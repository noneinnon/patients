(ns patients.core
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reitit.frontend :as rf]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]
   [reitit.coercion.spec :as rss]
   [re-frame.core :as re-frame]
   [patients.events :as events]
   [patients.subs :as subs]
   ["react-dom/client" :refer [createRoot]]))

(def log (.-log js/console))

(defn form-get [form key]
  (.get form key))

; views
;; ----------------------------------------------------------------------------
(defn header []
  [:header.flex.h-24.items-center.px-2
   [:h1.mb-2.uppercase.text-xl "The awesome Patients app"]
   [:nav.ml-auto.mb-2.flex.text-gray-600.items-center.gap-2
    [:a {:href (rfe/href :patients-list)} "Patients list"]
    [:a.p-2.px-4.bg-violet-700.text-white.rounded-lg.shadow-md {:href (rfe/href :new)} "Create"]]])

(defn filter-panel []
  [:form.flex.gap-2.p-2.mb-2.rounded-md.shadow-md
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (let [form (new js/FormData (.-target e))
                       filter-name (.get form "filter-name")
                       filter-value (.get form "filter-value")]
                   (re-frame/dispatch [::events/fetch-patients {filter-name filter-value}])))}

   [:select.grow {:name "filter-name"}
    [:option {:value "first_name"} "first name"]
    [:option {:value "last_name"} "last name"]
    [:option {:value "age"} "age"]
    [:option {:value "dob"} "date of birth"]
    [:option {:value "address"} "address"]
    [:option {:value "insurance_number"} "insurance number"]]
   [:input.p-2.grow {:name "filter-value" :placeholder "enter value"}]
   [:button.p-2.px-4.bg-violet-700.text-white.rounded-lg.shadow-s "filter"]])

(defn patient-row [{:keys [id first_name last_name insurance_number]}]
  [:tr {:class-name "p-1 hover:cursor-pointer hover:bg-gray-200"
        :key id
        :on-click (fn [e]
                    (let [target (.-target e)]
                      (if (= (.getAttribute target "name") "clickable")
                        (rfe/push-state :patient {:id id})
                        0)))}
   [:td.p-2 {:name "clickable"} id]
   [:td.p-2 {:name "clickable"} first_name]
   [:td {:name "clickable"} last_name]
   [:td {:name "clickable"} insurance_number]
   [:td.flex.items-center {:name "controls"}
    [:button {:on-click #(re-frame/dispatch [::events/delete-patient {:id id}])} "❌"]]])

(defn sort-handler [e]
  (let [target (.-target e)
        name (.getAttribute target "data-name")]
    (when name
      (rfe/push-state :patients-list {} {:sort-param name :order "asc"}))))

(defn patients-list [match]
  (let [params (:query-params match)
        patients (re-frame/subscribe [::subs/patients])]
    (reagent/create-class
     {:component-did-mount (fn []
                             (re-frame/dispatch [::events/fetch-patients params]))
      :reagent-render (fn []
                        [:section
                         [filter-panel]
                         (if (not-empty @patients)
                           [:table.w-full.mx-0.rounded-lg.shadow-md.border-1
                            [:thead.bg-gray-100.text-l.border-b-2
                             [:tr.hover:cursor-pointer {:on-click sort-handler}
                              [:th.text-left.font-thin.p-2 {:data-name "id"} "id"]
                              [:th.text-left.font-thin.p-2 {:data-name "first_name"} "first name"]
                              [:th.text-left.font-thin {:data-name "last_name"} "last name"]
                              [:th.text-left.font-thin {:data-name "insurance_number"} "insurance number"]
                              [:th.text-left.font-thin "remove"]]]
                            [:tbody
                             (map patient-row @patients)]]
                           [:p "No patients found"])])})))

(defn patient-view [initial-values on-submit]
  (let [state (reagent/atom initial-values)
        handle-change (fn [e]
                        (let [target (.-target e)
                              name (.-name target)
                              value (.-value target)]
                          (swap! state assoc (keyword name) value)))]
    (fn [] [:form.w-96.flex.flex-col.gap-2
            {:on-submit (fn [e]
                          (.preventDefault e)
                          (let [form (new js/FormData (.-target e))]
                            (on-submit form)))}
            [:label.flex.justify-between.items-center "First name"
             [:input.p-2.shadow-md.rounded-md {:on-change handle-change :value (:first_name @state) :name "first_name" :required true :type "text" :placeholder "First name"}]]
            [:label.flex.justify-between.items-center "Last name"
             [:input.p-2.shadow-md.rounded-md {:on-change handle-change :value (:last_name @state) :name "last_name" :required true :type "text" :placeholder "Last name"}]]
            [:label.flex.justify-between.items-center "Sex"
             [:input.p-2.shadow-md.rounded-md {:on-change handle-change :value (:sex @state) :name "sex" :required true :type "text" :placeholder "Sex"}]]
            [:label.flex.justify-between.items-center "Age"
             [:input.p-2.shadow-md.rounded-md.w-100 {:on-change handle-change :value (:age @state) :name "age" :required true :type "number" :placeholder "Age"}]]
            [:label.flex.justify-between.items-center "Date of birth"
             [:input.p-2.shadow-md.rounded-md {:on-change handle-change :value (:dob @state) :name "dob" :required true :type "date" :placeholder "Date of birth"}]]
            [:label.flex.justify-between.items-center "Insurance number"
             [:input.p-2.shadow-md.rounded-md {:on-change handle-change :value (:insurance_number @state) :name "insurance_number" :required true :type "text" :placeholder "Insrance number"}]]
            [:label.flex.justify-between.items-center "Address"
             [:input.p-2.shadow-md.rounded-md {:on-change handle-change :value (:address @state) :name "address" :required true :type "text" :placeholder "Address"}]]
            [:div.flex
             [:button {:on-click #(rfe/push-state :patients-list) :type "button"} "cancel"]
             [:button.p-2.px-4.bg-violet-700.rounded-md.text-white.ml-auto {:type "submit"} "ok"]]])))

(defn get-patient-form-data [form]
  {:first_name (form-get form "first_name")
   :last_name (form-get form "last_name")
   :sex (form-get form "sex")
   :age (form-get form "age")
   :dob (form-get form "dob")
   :insurance_number (form-get form "insurance_number")
   :address (form-get form "address")})

(defn create-new-patient []
  [patient-view {} (fn [form]
                     (re-frame/dispatch [::events/create-patient
                                         (get-patient-form-data form)]))])

(defn patient-page [match]
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
            [:section [patient-view @patient (fn [form] (re-frame/dispatch [::events/update-patient
                                                                            (assoc (get-patient-form-data form) :id patient-id)]))]]
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
    {:name :patients-list
     :view patients-list
     :controllers [{:parameters {:path [:id :first_name :last_name]}
                    :start (fn [parameters] (prn parameters))}]}]

   ["/new"
    {:name :new
     :view create-new-patient}]

   ["/patient/:id"
    {:name :patient
     :view patient-page
     :parameters {:path {:id int?}}}]])

;; main
;; ----------------------------------------------------------------------------

(defn- main []
  (fn []
    (rfe/start!
     (rf/router routes {:data {:coercion rss/coercion}})
     (fn [new-match]
       (swap! match
              (fn [old-match]
                (when new-match
                  (assoc new-match
                         :controllers (rfc/apply-controllers (:controllers old-match) new-match))))))
     ; (fn [m] (reset! match m))
          ;; set to false to enable HistoryAPI
     {:use-fragment false})
    [:main.p-2 [current-page]]))

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
