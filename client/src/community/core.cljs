(ns community.core
  (:require [community.state :refer [app-state]]
            [community.controller :as controller]
            [community.routes :as routes :refer [routes]]
            [community.api.push :as push-api]
            [community.components.app :as app]
            [community.components.index :as index]
            [community.components.subforum :as subforum]
            [community.components.thread :as thread]
            [community.components.shared :as shared]
            [community.components.settings :as settings]
            [om.core :as om]))

(enable-console-print!)

(defmethod routes/dispatch :index          [_] index/index)
(defmethod routes/dispatch :settings       [_] settings/settings)
(defmethod routes/dispatch :subforum       [_] subforum/subforum)
(defmethod routes/dispatch :thread         [_] thread/thread)
(defmethod routes/dispatch :page-not-found [_] shared/page-not-found)
(defmethod routes/dispatch :default        [_] shared/page-not-found)

;;; Exports
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn ^:export init-app
  "Mounts the om application onto target element."
  [target logo-url]
  (push-api/init-ws-connection! app-state)

  (controller/start-loop! app-state)

  (let [route-changed! #(controller/dispatch :route-changed
                          (routes (-> js/document .-location .-pathname)))]
    (route-changed!)
    (.addEventListener js/window "popstate" route-changed!))

  (om/root app/app
           app-state
           {:target target
            :shared {:logo-url logo-url}}))
