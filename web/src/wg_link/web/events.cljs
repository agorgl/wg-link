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

(def id-gen (atom 0))

(defn next-id []
  (swap! id-gen inc))

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
   (reset! id-gen (or (reduce max (map :id peers)) 0))
   (assoc db :peers (mapv #(-> %
                               (select-keys [:id :name :address :enabled])
                               (rename-keys {:address :ip})) peers))))

(re-frame/reg-event-fx
 ::add-peer
 (fn [{:keys [:db]} [_ name]]
   (let [next-id (next-id)]
     {:db (update-in db [:peers] #(->> %
                                       (concat [{:id next-id
                                                 :name name
                                                 :ip "<pending>"
                                                 :enabled true}])
                                       (sort-by :ip)
                                       (vec)))
      :http-xhrio {:method          :post
                   :uri             (str conf/api-url "/peers")
                   :params          {:name name}
                   :format          (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [::peer-added next-id]}})))

(re-frame/reg-event-db
 ::peer-added
 (fn [db [_ id peer]]
   (let [pidx (index-of #(= (:id %) id) (get-in db [:peers]))]
     (update-in db [:peers pidx] assoc :id (:id peer) :ip (:address peer)))))

(re-frame/reg-event-fx
 ::update-peer
 (fn [{:keys [:db]} [_ id params]]
   (let [pidx (index-of #(= (:id %) id) (get-in db [:peers]))]
     {:db (update-in db [:peers pidx] merge params)
      :http-xhrio {:method          :patch
                   :uri             (str conf/api-url "/peers/" id)
                   :params          (rename-keys params {:ip :address})
                   :format          (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [::peer-updated]}})))

(re-frame/reg-event-db
 ::peer-updated
 (fn [db _]
   db))

(re-frame/reg-event-fx
 ::delete-peer
 (fn [{:keys [:db]} [_ id]]
   {:db (update-in db [:peers]
                   (fn [peers]
                     (vec (remove #(= (:id %) id) peers))))
    :http-xhrio {:method          :delete
                 :uri             (str conf/api-url "/peers/" id)
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [::peer-deleted]}}))

(re-frame/reg-event-db
 ::peer-deleted
 (fn [db _]
   db))
