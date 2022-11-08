(ns wg-link.service.db
  (:require [wg-link.service.cidr :as cidr]
            [wg-link.service.wireguard :as wg]))

(defn- new-network [nm net]
  (let [[private-key public-key] (wg/keypair-gen)]
    {:name nm
     :network net
     :server {:address (first (cidr/available-ips net []))
              :private-key private-key
              :public-key public-key
              :domain "somedomain.com"
              :listen-port 56000}
     :peers []}))

(def peer-id-gen (atom 0))

(defn next-peer-id []
  (swap! peer-id-gen inc))

(defn- new-peer [nm addr]
  (let [[private-key public-key] (wg/keypair-gen)
        shared-key (wg/shared-key-gen)]
    {:id (next-peer-id)
     :name nm
     :address addr
     :private-key private-key
     :preshared-key shared-key
     :public-key public-key}))

(def db (atom (new-network "wg0" "10.5.5.0/24")))

(defn- allocate-peer-ip []
  (let [network (:network @db)
        peer-ips (map :address (:peers @db))
        server-ip (get-in @db [:server :address])
        occupied-ips (conj peer-ips server-ip)]
    (first (cidr/available-ips network occupied-ips))))

(defn peer-list []
  (->> (:peers @db)
       (map #(select-keys % [:id :name :address]))))

(defn peer-add [nm]
  (let [ip (allocate-peer-ip)
        np (new-peer nm ip)]
    (swap! db update-in [:peers]
           (fn [peers]
             (->> (conj peers np)
                  (sort-by :address)
                  (vec))))
    np))

(defn peer-get [id]
  (->> (:peers @db)
       (filter #(= (str (:id %)) id))
       (first)))

(defn peer-conf [id]
  (->> (:peers @db)
       (filter #(= (str (:id %)) id))
       (first)
       (wg/peer-conf (assoc (:server @db) :network (:network @db)))))

(defn peer-delete [id]
  (swap! db update-in [:peers]
         (fn [peers]
           (->> peers
                (remove #(= (str (:id %)) id))
                (sort-by :address)
                (vec)))))
