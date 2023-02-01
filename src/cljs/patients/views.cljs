(ns patients.views
  (:require
   [patients.events :as events]
   [patients.helpers :refer [get-patient-form-data]]
   [patients.subs :as subs]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [reitit.frontend.easy :as rfe]))

(defn loading-indicator []
  [:div "Loading"])

(defn header []
  [:header.flex.h-24.items-center.px-2
   [:h1.mb-2.uppercase.text-xl "The awesome Patients app"]
   [:nav.ml-auto.mb-2.flex.text-gray-600.items-center.gap-2
    [:a {:href (rfe/href :patients-list)} "Patients list"]
    [:a.p-2.px-4.bg-violet-700.text-white.rounded-lg.shadow-md {:href (rfe/href :new)} "Create"]]])

(defn filter-panel []
  (let [!input (atom nil)]
    [:form.flex.gap-2.p-2.mb-2.rounded-md.shadow-md
     {:on-submit (fn [e]
                   (.preventDefault e)
                   (let [form (new js/FormData (.-target e))
                         filter-name (keyword (.get form "filter-name"))
                         filter-value (.get form "filter-value")
                         params {filter-name filter-value}]
                     (when (not-empty filter-value) (rfe/push-state :patients-list {} params))))}

     [:select.grow {:name "filter-name"}
      [:option {:value "first_name"} "first name"]
      [:option {:value "last_name"} "last name"]
      [:option {:value "age"} "age"]
      [:option {:value "dob"} "date of birth"]
      [:option {:value "address"} "address"]
      [:option {:value "insurance_number"} "insurance number"]]
     [:input.p-2.grow {:name "filter-value"
                       :placeholder "enter value"
                       :ref (fn [el] (reset! !input el))}]
     [:button.p-2.px-4.bg-violet-700.text-white.rounded-lg.shadow-s {:type "button"
                                                                     :on-click (fn []
                                                                                 (rfe/push-state :patients-list)
                                                                                 (set! (.-value @!input) ""))}
      "cancel"]
     [:button.p-2.px-4.bg-violet-700.text-white.rounded-lg.shadow-s "filter"]]))

(defn pagination-panel []
  (let [query @(re-frame/subscribe [::subs/query])
        total @(re-frame/subscribe [::subs/total])
        limit (int (:limit query))
        offset (int (:offset query))
        next-offset (+ limit offset)
        prev-offset (max (- offset limit) 0)
        next-disabled (>= 0 (- total (+ offset limit)))]
    [:div.flex
     [:select {:value (:limit query)
               :on-change (fn [e] (rfe/push-state :patients-list {} {:limit (.. e -target -value)}))}
      [:option {:value 10} 10]
      [:option {:value 20} 20]
      [:option {:value 30} 30]]
     [:button.py-2.px-4.rounded-s {:disabled (= offset 0) :on-click (fn [] (rfe/push-state :patients-list {} (assoc query :offset prev-offset)))} "<"]
     [:button.py-2.px-4.rounded-s {:disabled next-disabled :on-click (fn [] (rfe/push-state :patients-list {} (assoc query :offset next-offset)))} ">"]]))

(defn patient-row [{:keys [id address sex first_name last_name insurance_number]}]
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
   [:td {:name "clickable"} sex]
   [:td {:name "clickable"} insurance_number]
   [:td {:name "clickable"} address]
   [:td.flex.items-center {:name "controls"}
    [:button {:on-click #(re-frame/dispatch [::events/delete-patient {:id id}])} "❌"]]])

(defn sort-handler [e]
  (let [query @(re-frame/subscribe [::subs/query])
        order (if (= (:order query) "asc")
                "desc"
                "asc")
        target (.-target e)
        name (.getAttribute target "data-name")
        params {:sort-param name :order order}]
    (when name
      (rfe/push-state :patients-list {} params)
      (re-frame/dispatch [::events/fetch-patients params]))))

(defn add-selected-styles [name]
  (let [query @(re-frame/subscribe [::subs/query])]
    (if (= (:sort-param query) name)
      "font-bold"
      "font-thin")))

(defn patients-list []
  (let [state (re-frame/subscribe [::subs/state])
        total (re-frame/subscribe [::subs/total])
        patients (re-frame/subscribe [::subs/patients])]
    (fn []
      [:section
       [filter-panel]
       (case @state
         :loading [loading-indicator]
         (if (not-empty @patients)
           [:div [:table.w-full.mx-0.rounded-lg.shadow-md.border-1
                  [:thead.bg-gray-100.text-l.border-b-2
                   [:tr.hover:cursor-pointer {:on-click sort-handler}
                    [:th.text-left.p-2 {:class (add-selected-styles "id") :data-name "id"} "id"]
                    [:th.text-left.p-2 {:class (add-selected-styles "first_name") :data-name "first_name"} "first name"]
                    [:th.text-left. {:class (add-selected-styles "last_name") :data-name "last_name"} "last name"]
                    [:th.text-left. {:class (add-selected-styles "sex") :data-name "sex"} "sex"]
                    [:th.text-left. {:class (add-selected-styles "insurance_number") :data-name "insurance_number"} "insurance number"]
                    [:th.text-left. {:class (add-selected-styles "address") :data-name "address"} "address"]
                    [:th.text-left.font-thin "remove"]]]
                  [:tbody
                   (map patient-row @patients)]]
            [:div.flex.justify-between.p-1
             [pagination-panel]
             [:p "Total: " @total]]]
           [:p "No patients found"]))])))

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
             [:input.p-2.shadow-md.rounded-md {:on-change handle-change :value (:dob @state) :name "dob" :required true :type "date" :format "yyyy-mm-dd" :placeholder "Date of birth"}]]
            [:label.flex.justify-between.items-center "Insurance number"
             [:input.p-2.shadow-md.rounded-md {:on-change handle-change :value (:insurance_number @state) :name "insurance_number" :required true :type "text" :placeholder "Insrance number"}]]
            [:label.flex.justify-between.items-center "Address"
             [:input.p-2.shadow-md.rounded-md {:on-change handle-change :value (:address @state) :name "address" :required true :type "text" :placeholder "Address"}]]
            [:div.flex
             [:button {:on-click #(rfe/push-state :patients-list) :type "button"} "cancel"]
             [:button.p-2.px-4.bg-violet-700.rounded-md.text-white.ml-auto {:type "submit"} "ok"]]])))

(defn create-new-patient []
  [patient-view {} (fn [form]
                     (re-frame/dispatch [::events/create-patient
                                         (get-patient-form-data form)]))])

(defn patient-page [match]
  (let [patient-id (get-in match [:path-params :id])
        patient (re-frame/subscribe [::subs/patient])
        state (re-frame/subscribe [::subs/state])]
    (fn []
      (case @state
        :loading [loading-indicator]
        (if @patient
          [:section [patient-view @patient (fn [form] (re-frame/dispatch [::events/update-patient
                                                                          (assoc (get-patient-form-data form) :id patient-id)]))]]
          [:section "No patient with this ID exist"])))))

(defn display-notifications [[id data]]
  [:div.p-5.rounded-md.shadow-md.mb-5 {:key id
                                       :class (get {:info "bg-green-500" :warning "bg-yellow-500" :error "bg-red-500"} (:level data) "bg-white")}
   [:p (:message data)]])

(defn notifications-list []
  (let [notifications @(re-frame/subscribe [::subs/notifications])]
    [:section.absolute.bottom-0 (map display-notifications notifications)]))
