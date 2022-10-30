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
   (map :name (:peers db))))

(re-frame/reg-sub
 ::peer
 (fn [db [_ id]]
   (filter #(= (:name %) id) (:peers db))))
