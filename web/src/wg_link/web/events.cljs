(ns wg-link.web.events
  (:require
   [re-frame.core :as re-frame]
   [wg-link.web.db :as db]))

(defn- index-of
  [pred coll]
  (first (keep-indexed #(when (pred %2) %1) coll)))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

(re-frame/reg-event-db
 ::update-peer-name
 (fn [db [_ peer name]]
   (let [pidx (index-of #(= (:name %) peer) (get-in db [:peers]))]
     (assoc-in db [:peers pidx :name] name))))

(re-frame/reg-event-db
 ::update-peer-ip
 (fn [db [_ peer ip]]
   (let [pidx (index-of #(= (:name %) peer) (get-in db [:peers]))]
     (assoc-in db [:peers pidx :ip] ip))))

(re-frame/reg-event-db
 ::enable-peer
 (fn [db [_ peer enable]]
   (let [pidx (index-of #(= (:name %) peer) (get-in db [:peers]))]
     (assoc-in db [:peers pidx :enabled] enable))))
