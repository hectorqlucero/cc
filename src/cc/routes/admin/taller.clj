(ns cc.routes.admin.taller
  (:require [cc.models.crud :refer :all]
            [cc.models.grid :refer :all]
            [cc.models.util :refer :all]
            [cheshire.core :refer :all]
            [compojure.core :refer :all]
            [selmer.parser :refer [render-file]]))

(defn taller
  []
  (render-file "admin/taller/index.html" {:title "Talleres"}))

;;start taller grid
(def search-columns
  ["id"
   "nombre"])

(def aliases-columns
  ["id"
   "nombre"
   "direccion"
   "telefono"
   "horarios"
   "sitio"
   "direcciones"
   "historia"])

(defn grid-json
  [{params :params}]
  (try
    (let [table    "taller"
          scolumns (convert-search-columns search-columns)
          aliases  aliases-columns
          join     ""
          search   (grid-search (:search params nil) scolumns)
          order    (grid-sort (:sort params nil) (:order params nil))
          order    (grid-sort-extra order "nombre")
          offset   (grid-offset (parse-int (:rows params)) (parse-int (:page params)))
          rows     (grid-rows table aliases join search order offset)]
      (generate-string rows))
    (catch Exception e (.getMessage e))))
;;end taller grid

;;start taller form
(def form-sql
  "SELECT * FROM taller WHERE id = ?")

(defn form-json
  [id]
  (let [row (Query db [form-sql id])]
    (generate-string (first row))))
;;end cuadrante form

(defn taller-save
  [{params :params}]
  (let [id       (fix-id (:id params))
        postvars {:id           id
                  :nombre (:nombre params)
                  :direccion (:direccion params)
                  :telefono (:telefono params)
                  :horarios (:horarios params)
                  :sitio (:sitio params)
                  :direcciones (:direcciones params)
                  :historia (:historia params)}
        result   (Save db :taller postvars ["id = ?" id])
        the-id   (if (nil? id) (get (first result) :generated_key nil) id)]
    (if (seq result)
      (generate-string {:success "Correctamente Processado!"})
      (generate-string {:error "No se pudo processar!"}))))

(defn taller-delete
  [{params :params}]
  (let [id (:id params nil)
        result (if-not (nil? id)
                 (Delete db :taller ["id = ?" id])
                 nil)]
    (if (seq result)
      (generate-string {:success "Removido appropiadamente!"})
      (generate-string {:error "No se pudo remover!"}))))

(defroutes taller-routes
  (GET "/admin/taller" [] (if-not (= (user-level) "U") (taller)))
  (POST "/admin/taller/json/grid" request [] (if-not (= (user-level) "U") (grid-json request)))
  (GET "/admin/taller/json/form/:id" [id] (if-not (= (user-level) "U") (form-json id)))
  (POST "/admin/taller/save" request [] (if-not (= (user-level) "U") (taller-save request)))
  (POST "/admin/taller/delete" request [] (if-not (= (user-level) "U") (taller-delete request))))
