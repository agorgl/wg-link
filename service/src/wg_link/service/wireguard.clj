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
