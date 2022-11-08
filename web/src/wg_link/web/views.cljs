(ns wg-link.web.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [wg-link.web.subs :as subs]
   [wg-link.web.events :as events]
   [wg-link.web.config :as conf]))

(defonce dialog (reagent/atom nil))

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
    [:button {:class "bg-red-800 text-white hover:bg-red-700 border-2 border-none py-2 px-4 rounded inline-flex items-center transition"
              :on-click #(reset! dialog :new-peer)}
     [icon-plus]
     [:span {:class "text-sm"}
      "New Peer"]]]])

(defn icon-qr []
  [:svg {:xmlns "http://www.w3.org/2000/svg"
         :fill "none"
         :viewBox "0 0 24 24"
         :stroke "currentColor"
         :class "w-5"}
   [:path {:stroke-linecap "round"
           :stroke-linejoin "round"
           :stroke-width "2"
           :d "M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z"}]])

(defn icon-download []
  [:svg {:xmlns "http://www.w3.org/2000/svg"
         :fill "none"
         :viewBox "0 0 24 24"
         :stroke "currentColor"
         :class "w-5"}
   [:path {:stroke-linecap "round"
           :stroke-linejoin "round"
           :stroke-width "2"
           :d "M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"}]])

(defn icon-delete []
  [:svg {:xmlns "http://www.w3.org/2000/svg"
         :viewBox "0 0 20 20"
         :fill "currentColor"
         :class "w-5"}
   [:path {:fill-rule "evenodd"
           :d "M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z"
           :clip-rule "evenodd"}]])

(defn peer-controls [{:keys [id name enabled]}]
  [:div {:class "text-gray-400 space-x-1"}
   [:div {:title (str (if enabled "Disable" "Enable") " Peer")
          :class "inline-block align-middle rounded-full w-10 h-6 mr-1 data-[enabled=true]:bg-red-800 data-[enabled=false]:bg-gray-200 cursor-pointer data-[enabled=true]:hover:bg-red-700 data-[enabled=false]:hover:bg-gray-300 transition-all group"
          :data-enabled enabled
          :on-click #(re-frame/dispatch [::events/update-peer id {:enabled (not enabled)}])}
    [:div {:class "rounded-full w-4 h-4 m-1 group-data-[enabled=true]:ml-5 bg-white"}]]
   [:button {:title "Show QR Code"
             :class "align-middle bg-gray-100 hover:bg-red-800 hover:text-white p-2 rounded transition"}
    [icon-qr]]
   [:a {:href (str conf/api-url "/peers/" id "/conf")
        :download (str name ".conf")
        :title "Download Configuration"
        :class "align-middle inline-block bg-gray-100 hover:bg-red-800 hover:text-white p-2 rounded transition"}
    [icon-download]]
   [:button {:title "Delete Peer"
             :class "align-middle bg-gray-100 hover:bg-red-800 hover:text-white p-2 rounded transition"
             :on-click #(re-frame/dispatch [::events/delete-peer id])}
    [icon-delete]]])

(defn icon-avatar []
  [:svg {:xmlns "http://www.w3.org/2000/svg"
         :viewBox "0 0 20 20"
         :fill "currentColor"
         :class "w-6 m-2 text-gray-300"}
   [:path {:fill-rule "evenodd"
           :d "M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z"
           :clip-rule "evenodd"}]])

(defn icon-edit []
  [:svg {:xmlns "http://www.w3.org/2000/svg"
         :fill "none"
         :viewBox "0 0 24 24"
         :stroke "currentColor"
         :class "h-4 w-4 inline align-middle opacity-25 hover:opacity-100"}
   [:path {:stroke-linecap "round"
           :stroke-linejoin "round"
           :stroke-width "2"
           :d "M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"}]])

(defn peer-name [id name]
  (let [edit (reagent/atom false)
        value (reagent/atom name)
        input (atom nil)
        submit (fn []
                 (when @edit
                   (reset! edit false)
                   (re-frame/dispatch [::events/update-peer id {:name @value}])))]
    (fn []
      [:div {:title "Created on Oct 28, 2022, 8:48 PM"
             :class "text-gray-700 group space-x-1"
             :data-edit @edit}
       [:input {:class "group-data-[edit=false]:hidden rounded px-1 border-2 border-gray-100 focus:border-gray-200 outline-none w-30"
                :value @value
                :on-change #(reset! value (-> % .-target .-value))
                :on-key-press #(when (= (.-key %) "Enter") (submit))
                :on-blur submit
                :ref #(reset! input %)}]
       [:span {:class "group-data-[edit=true]:hidden inline-block border-t-2 border-b-2 border-transparent"}
        @value]
       [:span {:class "cursor-pointer opacity-0 group-hover:opacity-100 transition-opacity"
               :on-click (fn []
                           (if (swap! edit not)
                             (js/setTimeout #(-> @input .focus) 1)
                             (submit)))}
        [icon-edit]]])))

(defn peer-ip [id ip]
  (let [edit (reagent/atom false)
        value (reagent/atom ip)
        input (atom nil)
        submit (fn []
                 (when @edit
                   (reset! edit false)
                   (re-frame/dispatch [::events/update-peer id {:ip @value}])))]
    [:div {:class "text-gray-400 text-xs"}
     [:span {:class "group space-x-1"
             :data-edit @edit}
      [:input {:class "group-data-[edit=false]:hidden rounded border-2 border-gray-100 focus:border-gray-200 outline-none w-20 text-black"
               :value @value
               :on-change #(reset! value (-> % .-target .-value))
               :on-key-press #(when (= (.-key %) "Enter") (submit))
               :on-blur submit
               :ref #(reset! input %)}]
      [:span {:class "group-data-[edit=true]:hidden inline-block border-t-2 border-b-2 border-transparent"}
       @value]
      [:span {:class "cursor-pointer opacity-0 group-hover:opacity-100 transition-opacity"
              :on-click (fn []
                          (if (swap! edit not)
                            (js/setTimeout #(-> @input .focus) 1)
                            (submit)))}
       [icon-edit]]]]))

(defn peer [{:keys [id name ip] :as p}]
  [:div {:class "relative overflow-hidden border-b border-gray-100 border-solid"}
   [:div {:class "relative p-5 z-10 flex flex-row"}
    [:div {:class "h-10 w-10 my-auto mr-4 rounded-full bg-gray-50 relative"}
     [icon-avatar]]
    [:div {:class "flex-grow"}
     [peer-name id name]
     [peer-ip id ip]]
    [:div {:class "text-right my-auto"}
     [peer-controls p]]]])

(defn peers []
  (let [peers (re-frame/subscribe [::subs/peer-ids])]
    (if (empty? @peers)
      [no-peers]
      [:div
       (doall
        (for [p @peers]
          ^{:key p} [peer @(re-frame/subscribe [::subs/peer p])]))])))

(defn content []
  [:div {:class "shadow-md rounded-lg bg-white overflow-hidden"}
   [:div {:class "flex flex-row flex-auto items-center p-3 px-5 border border-b-2 border-gray-100"}
    [:div {:class "flex-grow"}
     [:p {:class "text-2xl font-medium"}
      "Peers"]]
    [:div {:class "flex-shrink-0"}
     [:button {:class "hover:bg-red-800 hover:border-red-800 hover:text-white text-gray-700 border-2 border-gray-100 py-2 px-4 rounded inline-flex items-center transition"
               :on-click #(reset! dialog :new-peer)}
      [icon-plus]
      [:span {:class "text-sm"}
       "New"]]]]
   [peers]])

(defn icon-plus-big []
  [:svg {:inline ""
         :xmlns "http://www.w3.org/2000/svg"
         :fill "none"
         :viewBox "0 0 24 24"
         :stroke "currentColor"
         :class "h-6 w-6 text-white"}
   [:path {:stroke-linecap "round"
           :stroke-linejoin "round"
           :stroke-width "2"
           :d "M12 6v6m0 0v6m0-6h6m-6 0H6"}]])

(defn new-peer-dialog []
  (let [name (reagent/atom nil)
        submit (fn []
                 (reset! dialog nil)
                 (re-frame/dispatch [::events/add-peer @name]))]
    (fn []
      [:div {:class "fixed z-10 inset-0 overflow-y-auto"}
       [:div {:class "flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0"}
        [:div {:aria-hidden "true"
               :class "fixed inset-0 transition-opacity"}
         [:div {:class "absolute inset-0 bg-gray-500 opacity-75"}]]
        [:span {:aria-hidden "true"
                :class "hidden sm:inline-block sm:align-middle sm:h-screen"}]
        [:div {:role "dialog"
               :aria-modal "true"
               :aria-labelledby "modal-headline"
               :class "inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full"}
         [:div {:class "bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4"}
          [:div {:class "sm:flex sm:items-start"}
           [:div {:class "mx-auto flex-shrink-0 flex items-center justify-center h-12 w-12 rounded-full bg-red-800 sm:mx-0 sm:h-10 sm:w-10"}
            [icon-plus-big]]
           [:div {:class "flex-grow mt-3 text-center sm:mt-0 sm:ml-4 sm:text-left"}
            [:h3 {:id "modal-headline"
                  :class "text-lg leading-6 font-medium text-gray-900"}
             "New Peer"]
            [:div {:class "mt-2"}
             [:p {:class "text-sm text-gray-500"}
              [:input {:type "text"
                       :placeholder "Name"
                       :class "rounded p-2 border-2 border-gray-100 focus:border-gray-200 outline-none w-full"
                       :value @name
                       :on-change #(reset! name (-> % .-target .-value))
                       :on-key-press #(when (= (.-key %) "Enter") (submit))}]]]]]]
         [:div {:class "bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse"}
          [:button {:type "button"
                    :class "w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 disabled:bg-gray-200 enabled:bg-red-800 enabled:hover:bg-red-700 enabled:focus:outline-none text-base font-medium text-white sm:ml-3 sm:w-auto sm:text-sm disabled:cursor-not-allowed"
                    :disabled (empty? @name)
                    :on-click submit}
           "Create"]
          [:button {:type "button"
                    :class "mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm"
                    :on-click #(reset! dialog nil)}
           "Cancel"]]]]])))

(defn dialogs []
  (when-let [dialog @dialog]
    (case dialog
      :new-peer [new-peer-dialog])))

(defn app []
  [:div {:class "w-screen h-screen bg-gray-50 overflow-auto"}
   [:div {:class "container mx-auto max-w-3xl"}
    [header]
    [content]
    [dialogs]]])
