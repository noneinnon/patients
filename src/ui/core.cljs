(ns core
  (:require
   [reagent.core :as reagent :refer [atom]]
   ["react-dom/client" :refer [createRoot]]))

(defn- main []
  [:main
   [:h1 "hi"]])

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
