(ns wg-link.service.wireguard
  (:require [clojure.string :as str]
            [clojure.java.shell :refer [sh]]))

(defn keypair-gen []
  (let [private-key (str/trim-newline (:out (sh "wg" "genkey")))
        public-key (str/trim-newline (:out (sh "wg" "pubkey" :in private-key)))]
    [private-key public-key]))

(defn shared-key-gen []
  (let [shared-key (str/trim-newline (:out (sh "wg" "genpsk")))]
    shared-key))

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

(defn peer-sections [server peer]
  [[:interface {:private-key (:private-key peer)
                :address (str (:address peer) "/32")}]
   [:peer {:public-key (:public-key server)
           :preshared-key (:preshared-key peer)
           :allowed-ips (:network server)
           :persistent-keepalive 25
           :endpoint (str (:domain server) ":" (:listen-port server))}]])

(defn peer-conf [server peer]
  (->> (peer-sections server peer)
       (map (fn [[k v]] (ini-section k v)))
       (str/join "\n\n")))
