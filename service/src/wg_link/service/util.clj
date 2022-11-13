(ns wg-link.service.util
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.ring-middlewares :as middleware]
            [io.pedestal.interceptor.helpers :as interceptor]
            [ring.util.response :as response]
            [ring.util.mime-type :as mime])
  (:import java.io.File))

(defn index-of [pred coll]
  (first (keep-indexed #(when (pred %2) %1) coll)))

(defn merge-if-exists [m1 m2]
  (merge m1 (select-keys m2 (keys m1))))

(defn insert-at [coll f x]
  (->> coll
       (partition-by f)
       (apply (fn [a b c] (concat a (conj b x) c)))
       (vec)))

(defmethod response/resource-data :resource
  [^java.net.URL url]
  ;; GraalVM resource scheme
  (let [resource (.openConnection url)
        len (#'ring.util.response/connection-content-length resource)]
    (when (pos? len)
      {:content        (.getInputStream resource)
       :content-length len
       :last-modified  (#'ring.util.response/connection-last-modified resource)})))

(def file-content-type-interceptor
  (interceptor/after
   ::file-content-type-interceptor
   (fn [ctx]
     (let [resp (:response ctx)]
       (if-let [mime-type (or (get-in ctx [:response :headers "Content-Type"])
                              (when (instance? File (:body resp))
                                (mime/ext-mime-type (.getAbsolutePath ^File (:body resp)))))]
         (assoc-in ctx [:response :headers "Content-Type"] mime-type)
         ctx)))))

(defn extra-interceptors [service-map]
  (update-in service-map [::http/interceptors]
             insert-at #(= (:name %) ::middleware/content-type-interceptor) file-content-type-interceptor))
