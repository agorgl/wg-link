(ns wg-link.service.core
  (:gen-class) ; for -main method in uberjar
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]))

(defn hello-world
  [request]
  (let [name (get-in request [:params :name] "World")]
    {:status 200 :body (str "Hello " name "!\n")}))

(def routes
  #{["/greet" :get `hello-world]})

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
