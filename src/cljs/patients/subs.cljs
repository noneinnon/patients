(ns patients.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::loading
 (fn [db]
   (:loading db)))

(re-frame/reg-sub
 ::patients
 (fn [db]
   (:patients db)))

(re-frame/reg-sub
 ::patient
 (fn [db]
   (:patient db)))

(re-frame/reg-sub
 ::state
 (fn [db]
   (:state db)))

