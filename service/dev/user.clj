(ns user
  "Commonly used symbols for easy access in the Clojure REPL during development."
  (:require [clojure.repl :refer (apropos dir doc find-doc pst source)]
            [clojure.pprint :refer (pprint)]
            [clojure.string :as str]
            [wg-link.service.core :refer [run-dev]]
            [wg-link.service.db :as db]))

(comment
  (pprint (str/trim "This line suppresses some clj-kondo warnings.")))

; Init the network
(db/init-network "wg0" "10.5.5.0/24" "somedomain.com" 56000)

; Run dev server
(def dev-server (run-dev))
