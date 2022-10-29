(ns wg-link.web.views
  (:require
   [re-frame.core :as re-frame]
   [wg-link.web.subs :as subs]))

(defn header []
  [:h1 {:class "text-4xl font-medium my-10"}
   [:img {:src "./img/wireguard.svg" :width "32" :class "inline align-middle mr-2"}]
   [:span {:class "align-middle"}
    "WireGuard"]])

(defn hello []
  (let [name (re-frame/subscribe [::subs/name])]
    [:h1 "Hello from " @name]))

(defn content []
  [:div {:class "shadow-md rounded-lg bg-white overflow-hidden h-96"}
   [:div {:class "p-4"}
    [hello]]])

(defn app []
  [:div {:class "w-screen h-screen bg-gray-50 overflow-auto"}
   [:div {:class "container mx-auto max-w-3xl"}
    [header]
    [content]]])
