(ns cc.routes.admin.users
  (:require [cc.models.crud :refer :all]
            [cc.models.grid :refer :all]
            [cc.models.util :refer :all]
            [cheshire.core :refer :all]
            [clojure.string :refer [capitalize lower-case]]
            [compojure.core :refer :all]
            [noir.util.crypt :as crypt]
            [selmer.parser :refer [render-file]]))

(defn users [request]
  (render-file "admin/users/index.html" {:title "Usuarios"}))

;;start users grid
(def search-columns
  ["id"
   "lastname"
   "firstname"
   "username"
   "DATE_FORMAT(dob,'%m/%d/%Y')"
   "cell"
   "phone"
   "fax"
   "email"
   "CASE WHEN level ='A' THEN 'Administrador' WHEN level = 'U' THEN 'Usuario' END"
   "CASE WHEN active = 'T' THEN 'Activo' WHEN active = 'F' THEN 'Inactivo' END"])

(def aliases-columns
  ["id"
   "lastname"
   "firstname"
   "username"
   "password"
   "DATE_FORMAT(dob,'%m/%d/%Y') as dob"
   "cell"
   "phone"
   "fax"
   "email"
   "CASE WHEN level = 'A' THEN 'Administrador' WHEN level = 'U' THEN 'Usuario' END AS level"
   "CASE WHEN active = 'T' THEN 'Activo' WHEN active = 'F' THEN 'Inactivo' END AS active"])

(defn grid-json
  [request]
  (try
    (let [table    "users"
          scolumns (convert-search-columns search-columns)
          aliases  aliases-columns
          join     ""
          search   (grid-search (:search (:params request) nil) scolumns)
          order    (grid-sort (:sort (:params request) nil) (:order (:params request) nil))
          offset   (grid-offset (parse-int (:rows (:params request))) (parse-int (:page (:params request))))
          rows     (grid-rows table aliases join search order offset)]
      (generate-string rows))
    (catch Exception e (.getMessage e))))
;;end users grid

;;start users form
(def form-sql
  "SELECT id as id,
  lastname,
  firstname,
  username,
  password,
  DATE_FORMAT(dob,'%m/%d/%Y') as dob,
  cell,
  phone,
  fax,
  email,
  level,
  active
  FROM users
  WHERE id = ?")

(defn form-json [id]
  (let [record (Query db [form-sql id])]
    (generate-string (first record))))
;;end users form

(defn users-save [{params :params}]
  (let [id       (fix-id (:id params))
        postvars {:id        id
                  :lastname  (capitalize (:lastname params))
                  :firstname (capitalize (:firstname params))
                  :username  (lower-case (:username params))
                  :password  (if (> (count (:password params)) 60)
                               (:password params)
                               (crypt/encrypt (:password params)))
                  :dob       (format-date-internal (:dob params))
                  :cell      (:cell params nil)
                  :phone     (:phone params)
                  :fax       (:fax params)
                  :email     (lower-case (:email params))
                  :level     (:level params)
                  :active    (:active params "F")}
        result   (Save db :users postvars ["id = ? " id])]
    (if (seq result)
      (generate-string {:success "Correctamente processado!"})
      (generate-string {:error "No se pudo processar!"}))))

(defn users-delete [request]
  (let [id (:id (:params request))]
    (Delete db :users ["id = ?" id])
    (generate-string {:success "Removido appropiadamente!"})))

(defroutes users-routes
  (GET "/admin/users" request [] (if (= (user-level) "S") (users request)))
  (POST "/admin/users/json/grid" request [] (grid-json request))
  (GET "/admin/users/json/form/:id" [id] (form-json id))
  (POST "/admin/users/save" request [] (users-save request))
  (POST "/admin/users/delete" request [] (users-delete request)))

