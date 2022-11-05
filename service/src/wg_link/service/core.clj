(ns wg-link.service.core
  (:gen-class) ; for -main method in uberjar
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [wg-link.service.db :as db]))

(defn hello-world
  [request]
  (let [name (get-in request [:params :name] "World")]
    {:status 200 :body {:message (str "Hello " name "!")}}))

(defn peer-list [_]
  {:status 200 :body (db/peer-list)})

(defn peer-add [request]
  (let [nm (get-in request [:json-params :name])
        id (:id (db/peer-add nm))
        loc (route/url-for ::peer-get :params {:id id})]
    {:status 201 :headers {"Location" loc}}))

(defn peer-get [request]
  (let [id (get-in request [:path-params :id])]
    {:status 200 :body (db/peer-get id)}))

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
    ["/peers/:id/conf" :get    (conj common-interceptors `peer-conf)]
    ["/peers/:id"      :delete (conj common-interceptors `peer-delete)]})

(def service {:env                 :prod
              ::http/routes        routes
              ::http/resource-path "/public"
              ::http/type          :jetty
              ::http/port          8080})

(defn run-dev
  "The entry point for dev"
  [& _]
  (println "\nCreating your [DEV] server...")
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
      http/default-interceptors
      http/dev-interceptors
      http/create-server
      http/start))

;; This is an adapted service map, that can be started and stopped
;; From the REPL you can call http/start and http/stop on this service
(defonce runnable-service (http/create-server service))

(defn -main
  "The entry point"
  [& _]
  (println "\nCreating your server...")
  (http/start runnable-service))
