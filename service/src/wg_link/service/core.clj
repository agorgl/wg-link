(ns wg-link.service.core
  (:gen-class) ; for -main method in uberjar
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [wg-link.service.db :as db]
            [wg-link.service.util :as util]))

(defn hello-world
  [request]
  (let [name (get-in request [:params :name] "World")]
    {:status 200 :body {:message (str "Hello " name "!")}}))

(def peer-keys [:id :name :address :enabled])

(defn peer-list [_]
  {:status 200 :body (db/peer-list)})

(defn peer-add [request]
  (let [nm (get-in request [:json-params :name])
        peer (-> (db/peer-add nm)
                 (select-keys peer-keys))
        loc (route/url-for ::peer-get :params {:id (:id peer)})]
    {:status 201 :body peer :headers {"Location" loc}}))

(defn peer-get [request]
  (let [id (get-in request [:path-params :id])]
    {:status 200 :body (db/peer-get id)}))

(defn peer-update [request]
  (let [id (get-in request [:path-params :id])
        peer (-> (db/peer-update id (:json-params request))
                 (select-keys peer-keys))]
    {:status 200 :body peer}))

(defn peer-conf [request]
  (let [id (get-in request [:path-params :id])]
    {:status 200 :body (db/peer-conf id)}))

(defn peer-delete [request]
  (let [id (get-in request [:path-params :id])]
    (db/peer-delete id)
    {:status 204}))

(def common-interceptors [(body-params/body-params) http/json-body])

(def routes
  #{["/greet"          :get    (conj common-interceptors `hello-world)]
    ["/peers"          :get    (conj common-interceptors `peer-list)]
    ["/peers"          :post   (conj common-interceptors `peer-add)]
    ["/peers/:id"      :get    (conj common-interceptors `peer-get)]
    ["/peers/:id"      :patch  (conj common-interceptors `peer-update)]
    ["/peers/:id/conf" :get    (conj common-interceptors `peer-conf)]
    ["/peers/:id"      :delete (conj common-interceptors `peer-delete)]})

(def service {:env                  :prod
              ::http/routes         routes
              ::http/file-path      "resources/public/"
              ::http/secure-headers {:content-security-policy-settings {:object-src "'none'"}}
              ::http/type           :jetty
              ::http/host           "0.0.0.0"
              ::http/port           8080})

(defn global-interceptors [service-map]
  (-> service-map
      http/default-interceptors
      util/extra-interceptors))

(defn run-dev
  "The entry point for dev"
  [& _]
  (println "Creating your [DEV] server...")
  (-> service ;; Start with production configuration
      (merge {:env :dev
              ;; Do not block thread that starts web server
              ::http/join? false
              ;; Routes can be a function that resolve routes,
              ;; we can use this to set the routes to be reloadable
              ::http/routes #(route/expand-routes (deref #'routes))
              ;; All origins are allowed in dev mode
              ::http/allowed-origins {:creds true :allowed-origins (constantly true)}})
      ;; Wire up interceptor chains
      global-interceptors
      http/dev-interceptors
      http/create-server
      http/start))

;; This is an adapted service map, that can be started and stopped
;; From the REPL you can call http/start and http/stop on this service
(defonce runnable-service (http/create-server (-> service global-interceptors)))

(defn -main
  "The entry point"
  [& _]
  (println "Creating your server...")
  (db/init-network "wg0" "10.5.5.0/24" "somedomain.com" 56000)
  (http/start runnable-service))
