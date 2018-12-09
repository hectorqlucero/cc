(ns cc.core
  (:gen-class)
  (:require [cc.models.crud :refer :all]
            [cc.routes.admin :refer [admin-routes]]
            [cc.routes.entrenamiento :refer [entrenamiento-routes]]
            [cc.routes.cartas :refer [cartas-routes]]
            [cc.routes.calendario :refer [calendario-routes]]
            [cc.routes.home :refer [home-routes]]
            [cc.routes.table_ref :refer [table_ref-routes]]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [noir.response :refer [redirect]]
            [noir.session :as session]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.multipart-params :refer :all]
            [ring.middleware.reload :as reload]
            [ring.middleware.session :refer :all]
            [ring.middleware.session.cookie :refer :all]
            [ring.util.anti-forgery :refer :all]
            [selmer.filters :refer :all]
            [selmer.parser :refer :all]))

(set-resource-path! (clojure.java.io/resource "templates"))
(add-filter! :format-title (fn [x] [:safe (clojure.string/replace x #"'" "&#145;")]))
(add-tag! :csrf-field (fn [_ _] (anti-forgery-field)))
(add-tag! :username
          (fn [_ _]
            (str (if (session/get :user_id) (:username (first (Query db ["select username from users where id=?" (session/get :user_id)]))) "Anonimo"))))
(add-tag! :site_name
          (fn [_ _]
            (str (:site-name config))))

(add-tag! :user_status
          (fn [_ -]
            (let [user-id (session/get :user_id nil)
                  nivel (if user-id (:level (first (Query db ["SELECT level FROM users WHERE id = ?" user-id]))))]
              (if user-id
                (do
                  (case nivel
                    "A" (str
                          "<li class=\"nav-item\"><a href=\"/admin/cuadrantes\" class=\"nav-link\">Cuadrantes</a></li>"
                          "<li class=\"nav-item\"><a href=\"/logoff\" class=\"nav-link\">Salir</a></li>")
                    "S" (str
                          "<li class=\"nav-item\"><a href=\"/admin/cuadrantes\" class=\"nav-link\">Cuadrantes</a></li>"
                          "<li class=\"nav-item\"><a href=\"/admin/users\" class=\"nav-link\">Usuarios</a></li>"
                          "<li class=\"nav-item\"><a href=\"/logoff\" class=\"nav-link\">Salir</a></li>")
                    "U" (str "<li class=\"nav-item\"><a href=\"/logoff\" class=\"nav-link\">Salir</a></li>"))
                  )
                (str "<li class=\"nav-item\"><a href=\"/login\" class=\"nav-link\">Entrar</a></li>")))))

(defn wrap-login [hdlr]
  (fn [req]
    (try
      (if (nil? (session/get :user_id)) (redirect "/") (hdlr req))
      (catch Exception _
        {:status 400 :body "Unable to process your request!"}))))

(defn wrap-exception-handling [hdlr]
  (fn [req]
    (try
      (hdlr req)
      (catch Exception _
        {:status 400 :body "Invalid data"}))))

(defroutes public-routes
  home-routes
  entrenamiento-routes
  cartas-routes
  calendario-routes
  table_ref-routes)

(defroutes protected-routes
  admin-routes)

(defroutes app-routes
  (route/resources "/")
  (route/files "/uploads/" {:root (:uploads config)})
  (route/not-found "Not Found"))

(defn -main []
  (jetty/run-jetty
   (-> (routes
        public-routes
        (wrap-login protected-routes)
        (wrap-exception-handling protected-routes)
        app-routes)
       (handler/site)
       (wrap-session)
       (session/wrap-noir-session*)
       (wrap-multipart-params)
       (reload/wrap-reload)
       (wrap-defaults (-> site-defaults
                          (assoc-in [:security :anti-forgery] true)
                          (assoc-in [:session :store] (cookie-store {:key KEY}))
                          (assoc-in [:session :cookie-attrs] {:max-age 18000})
                          (assoc-in [:session :cookie-name] "LS"))))
   {:port (:port config)}))
