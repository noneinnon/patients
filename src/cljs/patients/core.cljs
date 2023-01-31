(ns patients.core
  (:require
   ["react-dom/client" :refer [createRoot]]
   [patients.events :as events]
   [patients.routes :refer [current-page start-router]]
   [patients.views :refer [notifications-list]]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]))

;; main
;; ----------------------------------------------------------------------------

(defn- main []
  (fn []
    (start-router)
    [:main.p-2 [current-page] [notifications-list]]))

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
