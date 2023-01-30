(ns patients.subs
  (:require
   [re-frame.core :as re-frame]))

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

(re-frame/reg-sub
  ::match
  (fn [db]
    (:match db)))

(re-frame/reg-sub
  ::query
  (fn [db]
    (get-in db [:match :query-params])))
