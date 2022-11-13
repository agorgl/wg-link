(ns wg-link.service.db
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [io.pedestal.log :as log]
            [wg-link.service.cidr :as cidr]
            [wg-link.service.wireguard :as wg]
            [wg-link.service.util :refer [index-of merge-if-exists]]))

(defn- new-network [nm net domain port]
  (let [[private-key public-key] (wg/keypair-gen)]
    {:name nm
     :network net
     :server {:address (first (cidr/available-ips net []))
              :private-key private-key
              :public-key public-key
              :domain domain
              :listen-port port}
     :peers []}))

(def peer-id-gen (atom 0))

(defn- next-peer-id []
  (swap! peer-id-gen inc))

(defn- new-peer [nm addr]
  (let [[private-key public-key] (wg/keypair-gen)
        shared-key (wg/shared-key-gen)]
    {:id (next-peer-id)
     :name nm
     :address addr
     :private-key private-key
     :preshared-key shared-key
     :public-key public-key
     :enabled true}))

(def db (atom nil))

(defn- allocate-peer-ip []
  (let [network (:network @db)
        peer-ips (map :address (:peers @db))
        server-ip (get-in @db [:server :address])
        occupied-ips (conj peer-ips server-ip)]
    (first (cidr/available-ips network occupied-ips))))

(defn db-file [nm]
  (str wg/conf-dir "/" nm ".json"))

(defn- load-db [nm]
  (-> (db-file nm)
      (slurp)
      (json/read-str :key-fn keyword)))

(defn- persist-db [db]
  (let [nm (:name db)
        db-file (db-file nm)]
    (io/make-parents db-file)
    (->> db
         (json/write-str)
         (spit db-file))))

(defn- update-db! [f]
  (let [db (f db)]
    (persist-db db)
    (wg/update-conf (merge (:server db) (select-keys db [:name :network]))
                    (:peers db))
    (wg/reload-interface (:name db))))

(defn init-network [nm net domain port]
  (let [db-file (db-file nm)
        db-exists (.exists (io/as-file db-file))]
    (update-db!
     (fn [db]
       (reset! db
               (if db-exists
                 (do
                   (log/info :msg (str "Found database for " nm " loading..."))
                   (load-db nm))
                 (new-network nm net domain port)))))))

(defn peer-list []
  (->> (:peers @db)
       (map #(select-keys % [:id :name :address :enabled]))))

(defn peer-add [nm]
  (let [ip (allocate-peer-ip)
        np (new-peer nm ip)]
    (update-db!
     (fn [db]
       (swap! db update-in [:peers]
              (fn [peers]
                (->> (conj peers np)
                     (sort-by :address)
                     (vec))))))
    np))

(defn peer-get [id]
  (->> (:peers @db)
       (filter #(= (str (:id %)) id))
       (first)))

(defn peer-update [id peer]
  (let [pidx (index-of #(= (str (:id %)) id) (get-in @db [:peers]))
        up (merge-if-exists (get-in @db [:peers pidx]) peer)]
    (update-db!
     (fn [db]
       (swap! db assoc-in [:peers pidx] up)))
    up))

(defn peer-conf [id]
  (->> (:peers @db)
       (filter #(= (str (:id %)) id))
       (first)
       (wg/peer-conf (assoc (:server @db) :network (:network @db)))))

(defn peer-delete [id]
  (update-db!
   (fn [db]
     (swap! db update-in [:peers]
            (fn [peers]
              (->> peers
                   (remove #(= (str (:id %)) id))
                   (sort-by :address)
                   (vec)))))))
