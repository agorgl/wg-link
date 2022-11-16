(ns wg-link.service.wireguard
  (:require [clojure.string :as str]
            [clojure.java.io :refer [make-parents]]
            [clojure.java.shell :refer [sh]]
            [io.pedestal.log :as log]))

(defn keypair-gen []
  (let [private-key (str/trim-newline (:out (sh "wg" "genkey")))
        public-key (str/trim-newline (:out (sh "wg" "pubkey" :in private-key)))]
    [private-key public-key]))

(defn shared-key-gen []
  (let [shared-key (str/trim-newline (:out (sh "wg" "genpsk")))]
    shared-key))

(defn reload-interface [nm]
  (log/info :msg (str "Reloading interface " nm))
  (let [down-out (:err (sh "wg-quick" "down" nm))
        up-out (:err (sh "wg-quick" "up" nm))]
    (doseq [m (concat (str/split-lines down-out)
                      (str/split-lines up-out))]
      (log/info :msg m))))

(def special-mappings
  {:allowed-ips "AllowedIPs"})

(defn kebab-case->PascalCase [s]
  (let [words (str/split s #"-")]
    (->> (map str/capitalize words)
         (apply str))))

(defn keyword->strkey [k]
  (let [m special-mappings]
    (get m k (kebab-case->PascalCase (name k)))))

(defn ini-section [title pairs]
  (let [fmtv #(if (coll? %) (str/join ", " %) %)
        lines (concat
               [(str "[" (keyword->strkey title) "]")]
               (map (fn [[k v]] (str (keyword->strkey k) " = " (fmtv v))) pairs))]
    (str/join "\n" lines)))

(defn peer-sections [cidr server peer]
  [[:interface {:private-key (:private-key peer)
                :address (str (:address peer) "/32")}]
   [:peer {:public-key (:public-key server)
           :preshared-key (:preshared-key peer)
           :allowed-ips cidr
           :persistent-keepalive 25
           :endpoint (str (:domain server) ":" (:listen-port server))}]])

(defn peer-conf [cidr server peer]
  (->> (peer-sections cidr server peer)
       (map (fn [[k v]] (ini-section k v)))
       (str/join "\n\n")))

(defn server-sections [cidr server peers]
  (concat
   [[:interface {:private-key (:private-key server)
                 :address (str (:address server) "/" (second (str/split cidr #"/")))
                 :listen-port (:listen-port server)}]]
   (map (fn [peer]
          [:peer {:public-key (:public-key peer)
                  :preshared-key (:preshared-key peer)
                  :allowed-ips (into [(str (:address peer) "/32")] (:gateway-ips peer))}])
        peers)))

(defn server-conf [cidr server peers]
  (let [enabled-peers (filter :enabled peers)]
    (->> (server-sections cidr server enabled-peers)
         (map (fn [[k v]] (ini-section k v)))
         (str/join "\n\n"))))

(def conf-dir "/etc/wireguard")

(defn update-conf [network]
  (let [conf-file (str conf-dir "/" (:name network) ".conf")]
    (make-parents conf-file)
    (->> (server-conf (:cidr network) (:server network) (:peers network))
         (spit conf-file))))
