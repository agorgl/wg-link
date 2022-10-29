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

(defn icon-plus []
  [:svg {:inline ""
         :xmlns "http://www.w3.org/2000/svg"
         :fill "none"
         :viewBox "0 0 24 24"
         :stroke "currentColor"
         :class "w-4 mr-2"}
   [:path {:stroke-linecap "round"
           :stroke-linejoin "round"
           :stroke-width "2"
           :d "M12 6v6m0 0v6m0-6h6m-6 0H6"}]])

(defn no-peers []
  [:div
   [:p {:class "text-center m-10 text-gray-400 text-sm"}
    "There are no peers yet."
    [:br]
    [:br]
    [:button {:class "bg-red-800 text-white hover:bg-red-700 border-2 border-none py-2 px-4 rounded inline-flex items-center transition"}
     [icon-plus]
     [:span {:class "text-sm"}
      "New Peer"]]]])

(defn peers []
  (let [peers (re-frame/subscribe [::subs/peers])]
    (if (empty? @peers)
      [no-peers]
      [:div {:class "m-8"}
       [hello]])))

(defn content []
  [:div {:class "shadow-md rounded-lg bg-white overflow-hidden"}
   [:div {:class "flex flex-row flex-auto items-center p-3 px-5 border border-b-2 border-gray-100"}
    [:div {:class "flex-grow"}
     [:p {:class "text-2xl font-medium"}
      "Peers"]]
    [:div {:class "flex-shrink-0"}
     [:button {:class "hover:bg-red-800 hover:border-red-800 hover:text-white text-gray-700 border-2 border-gray-100 py-2 px-4 rounded inline-flex items-center transition"}
      [icon-plus]
      [:span {:class "text-sm"}
       "New"]]]]
   [peers]])

(defn app []
  [:div {:class "w-screen h-screen bg-gray-50 overflow-auto"}
   [:div {:class "container mx-auto max-w-3xl"}
    [header]
    [content]]])
