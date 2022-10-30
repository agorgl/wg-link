(ns wg-link.web.db)

(def default-db
  {:name "re-frame"
   :peers
   [{:name "peer-1"
     :ip "10.5.5.1"
     :enabled true}
    {:name "peer-2"
     :ip "10.5.5.2"
     :enabled true}
    {:name "peer-3"
     :ip "10.5.5.3"
     :enabled false}]})
