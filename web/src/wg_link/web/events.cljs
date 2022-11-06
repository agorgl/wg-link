(ns wg-link.web.events
  (:require
   [clojure.set :refer [rename-keys]]
   [re-frame.core :as re-frame]
   [ajax.core :as ajax]
   [wg-link.web.db :as db]
   [wg-link.web.config :as conf]))

(defn- index-of
  [pred coll]
  (first (keep-indexed #(when (pred %2) %1) coll)))

(defn- allocate-ip [ips]
  (->> ips
       (map #(re-matches #"(\d+\.\d+\.\d+\.)(\d)" %))
       (map rest)
       (sort-by second)
       (#(concat [`(~(first (first %)) "0")] %))
       (partition 2 1 [nil])
       (filter (fn [[[_ a] [_ b]]] (not= 1 (- b a))))
       (first)
       (first)
       (apply #(str %1 (inc (js/parseInt %2))))))

(re-frame/reg-event-fx
 ::initialize-db
 (fn [_]
   {:db db/default-db
    :dispatch [::fetch-peers]}))

(re-frame/reg-event-fx
 ::fetch-peers
 (fn [_]
   {:http-xhrio {:method          :get
                 :uri             (str conf/api-url "/peers")
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [::peers-fetched]}}))

(re-frame/reg-event-db
 ::peers-fetched
 (fn [db [_ peers]]
   (assoc db :peers (mapv #(-> %
                               (select-keys [:name :address])
                               (rename-keys {:address :ip})
                               (merge {:enabled true})) peers))))

(re-frame/reg-event-db
 ::add-peer
 (fn [db [_ peer]]
   (let [peer-ips (map :ip (:peers db))
         ip (allocate-ip peer-ips)]
     (update-in db [:peers] #(->> %
                                  (concat [{:name peer
                                            :ip ip
                                            :enabled true}])
                                  (sort-by :ip)
                                  (vec))))))

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

(re-frame/reg-event-db
 ::delete-peer
 (fn [db [_ peer]]
   (update-in db [:peers]
              (fn [peers]
                (vec (remove #(= (:name %) peer) peers))))))
