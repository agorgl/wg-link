(ns user
  "Commonly used symbols for easy access in the Clojure REPL during development."
  (:require [clojure.repl :refer (apropos dir doc find-doc pst source)]
            [clojure.pprint :refer (pprint)]
            [clojure.string :as str]
            [wg-link.service.core :refer [run-dev]]))

(comment
  (pprint (str/trim "This line suppresses some clj-kondo warnings.")))

; Run dev server
(def dev-server (run-dev))
