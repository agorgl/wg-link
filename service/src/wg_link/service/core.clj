(ns wg-link.service.core
  (:gen-class) ; for -main method in uberjar
  (:require [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [wg-link.service.db :as db]
            [wg-link.service.util :as util]))

(defn hello-world
  [request]
  (let [name (get-in request [:params :name] "World")]
    {:status 200 :body {:message (str "Hello " name "!")}}))

(def peer-keys [:id :name :address :gateway-ips :enabled])

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

(def cli-options
  [["-n" "--name NAME" "Network name"
    :default "wg0"]
   ["-c" "--cidr CIDR" "Network cidr"
    :default "10.5.5.0/24"]
   ["-r" "--hostname HOST" "Remote host"
    :missing "Missing required HOST argument"]
   ["-p" "--port PORT" "Port number"
    :default 51820
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["Usage: wg-easy [options]"
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n"
       (str/join \newline errors)))

(defn validate-args [args]
  (let [{:keys [options errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) {:exit-message (usage summary) :ok? true}
      errors          {:exit-message (error-msg errors)}
      :else           {:options options})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main
  "The entry point"
  [& args]
  (let [{:keys [options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (do
        (println "Creating your server...")
        (db/init-network (:name options) (:cidr options) (:host options) (:port options))
        (http/start runnable-service)))))
