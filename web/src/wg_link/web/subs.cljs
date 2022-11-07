(ns wg-link.web.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 ::peer-ids
 (fn [db]
   (map :id (:peers db))))

(re-frame/reg-sub
 ::peer
 (fn [db [_ id]]
   (filter #(= (:id %) id) (:peers db))))
