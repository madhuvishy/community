(ns community.components.thread
  (:require [community.state :as state]
            [community.controller :as controller]
            [community.util :as util :refer-macros [<? p]]
            [community.models :as models]
            [community.partials :as partials :refer [link-to]]
            [community.routes :as routes]
            [community.components.shared :as shared]
            [om.core :as om]
            [om-tools.core :refer-macros [defcomponent]]
            [sablono.core :refer-macros [html]]
            [cljs.core.async :as async]
            [clojure.string :as str])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defcomponent post-form [{:keys [post index autocomplete-users broadcast-groups cancel-edit]} owner]
  (display-name [_] "PostForm")

  (init-state [_]
    {:original-post-body (:body post)})

  (render [_]
    (html
      [:div
       (let [errors (:errors post)]
         (if (not (empty? errors))
           [:div (map (fn [e] [:p.text-danger e]) errors)]))
       [:form {:onSubmit (fn [e]
                           (.preventDefault e)
                           (when-not (:submitting? @post)
                             (if (:persisted? @post)
                               (controller/dispatch :update-post @post index)
                               (controller/dispatch :new-post @post))))}
        [:div.post-form-body
         (when (not (:persisted? post))
           [:div.form-group
            (shared/->broadcast-group-picker
             {:broadcast-groups (mapv #(assoc % :selected? (contains? (:broadcast-to post) (:id %)))
                                      broadcast-groups)}
             {:opts {:on-toggle (fn [id]
                                  (om/transact! post :broadcast-to #(models/toggle-broadcast-to % id)))}})])
         (let [post-body-id (str "post-body-" (or (:id post) "new"))]
           [:div.form-group
            [:label.hidden {:for post-body-id} "Body"]
            (shared/->autocompleting-textarea
             {:value (:body post)
              :autocomplete-list (mapv :name autocomplete-users)}
             {:opts {:focus? (:persisted? post)
                     :on-change #(om/update! post :body %)
                     :passthrough
                     {:id post-body-id
                      :class ["form-control" "post-textarea"]
                      :name "post[body]"
                      :data-new-anchor true
                      :placeholder "Compose your post..."}}})])]
        [:div.post-form-controls
         [:button.btn.btn-default.btn-sm {:type "submit"
                                          :disabled (:submitting? post)}
          (if (:persisted? post) "Update" "Post")]
         (when (:persisted? post)
           [:button.btn.btn-link.btn-sm {:type "button"
                                         :onClick (fn [e]
                                                    (om/update! post :body (om/get-state owner :original-post-body))
                                                    (cancel-edit))}
            "Cancel"])]]])))

(defn wrap-mentions
  "Wraps @mentions in a post body in <span class=\"at-mention\">"
  [body users]
  (models/replace-mentions body users (fn [name]
                                        (str "<span class=\"at-mention\">" name "</span>"))))

(defcomponent post [{:keys [post index autocomplete-users]} owner]
  (display-name [_] "Post")

  (render [_]
    (html
      [:li.post {:key (:id post)}
       [:div.row
        [:div.post-author-image
         [:a {:href (routes/hs-route :person (:author post))}
          [:img
           {:src (-> post :author :avatar-url)
            :width "50"       ;TODO: request different image sizes
            }]]]
        [:div.post-metadata
         [:a {:href (routes/hs-route :person (:author post))}
          (-> post :author :name)]
         [:div (-> post :author :batch-name)]
         [:div (util/human-format-time (:created-at post))]]
        [:div.post-content
         (if (:editing? post)
           (->post-form {:post post
                         :index index
                         :autocomplete-users autocomplete-users
                         :cancel-edit #(om/update! post :editing? false)})
           [:div.row
            [:div.post-body
             (partials/html-from-markdown
              (wrap-mentions (:body post) autocomplete-users))]
            [:div.post-controls
             (when (and (:editable post) (not (:editing? post)))
               [:button.btn.btn-default.btn-sm
                {:onClick (fn [e]
                            (.preventDefault e)
                            (om/update! post :editing? true))}
                "Edit"])]])]]])))

(defcomponent thread [{:keys [thread]} owner]
  (display-name [_] "Thread")

  (render [this]
    (let [autocomplete-users (:autocomplete-users thread)]
      (html
        [:div
         (partials/title (:title thread) "New post")
         (shared/->subscription-info (:subscription thread))
         [:ol.list-unstyled
          (for [[i post] (map-indexed vector (:posts thread))]
            (->post {:post post :autocomplete-users autocomplete-users :index i}
                    {:react-key (:id post)}))]
         [:div.panel.panel-default
          [:div.panel-heading
           [:span.title-caps "New post"]]
          [:div.panel-body
           (->post-form {:broadcast-groups (:broadcast-groups thread)
                         :autocomplete-users autocomplete-users
                         :post (assoc (:new-post thread)
                                 :errors (:errors thread)
                                 :submitting? (:submitting? thread))})]]]))))
