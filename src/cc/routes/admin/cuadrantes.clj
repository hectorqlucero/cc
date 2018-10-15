(ns cc.routes.admin.cuadrantes
  (:require [cc.models.crud :refer :all]
            [cc.models.grid :refer :all]
            [cc.models.util :refer :all]
            [cheshire.core :refer :all]
            [clojure.string :refer [capitalize]]
            [compojure.core :refer :all]
            [selmer.parser :refer [render-file]]))

(defn cuadrantes
  [request]
  (render-file "admin/cuadrantes/index.html" {:title "Cuadrantes"}))

;;start cuadrantes grid
(def search-columns
  ["id"
   "name"
   "leader"
   "leader_phone"
   "leader_cell"
   "leader_email"
   "notes"
   "CASE WHEN status = 'T' THEN 'Activo' WHEN status = 'F' THEN 'Inactivo' ELSE '??' END"])

(def aliases-columns
  ["id"
   "name"
   "leader"
   "leader_phone"
   "leader_cell"
   "leader_email"
   "notes"
   "CASE WHEN status = 'T' THEN 'Activo' WHEN status = 'F' THEN 'Inactivo' ELSE '??' END AS status"])

(defn grid-json
  [{params :params}]
  (try
    (let [table    "cuadrantes"
          scolumns (convert-search-columns search-columns)
          aliases  aliases-columns
          join     ""
          search   (grid-search (:search params nil) scolumns)
          order    (grid-sort (:sort params nil) (:order params nil))
          offset   (grid-offset (parse-int (:rows params)) (parse-int (:page params)))
          rows     (grid-rows table aliases join search order offset)]
      (generate-string rows))
    (catch Exception e (.getMessage e))))
;;end cuadrantes grid

;;start cuadrantes form
(def form-sql
  "SELECT id as id,
  name,
  leader,
  leader_phone,
  leader_cell,
  leader_email,
  notes,
  status
  FROM cuadrantes
  WHERE id = ?")

(defn form-json
  [id]
  (let [row (Query db [form-sql id])]
    (generate-string (first row))))
;;end cuadrante form

(defn cuadrantes-save
  [{params :params}]
  (let [id       (fix-id (:id params))
        postvars {:id           id
                  :name         (:name params)
                  :leader       (capitalize (:leader params))
                  :leader_phone (:leader_phone params)
                  :leader_cell  (:leader_cell params)
                  :leader_email (:leader_email params)
                  :notes        (:notes params)
                  :status       (:status params "F")}
        result   (Save db :cuadrantes postvars ["id = ? " id])]
    (if (seq result)
      (generate-string {:success "Correctamente Processado!"})
      (generate-string {:error "No se pudo processar!"}))))

(defn cuadrantes-delete
  [{params :params}]
  (let [id (:id params nil)
        result (if-not (nil? id)
                 (Delete db :cuadrantes ["id = ?" id])
                 nil)]
    (if (seq result)
      (generate-string {:success "Removido appropiadamente!"})
      (generate-string {:error "No se pudo remover!"}))))

(defroutes cuadrantes-routes
  (GET "/admin/cuadrantes" request [] (cuadrantes request))
  (POST "/admin/cuadrantes/json/grid" request [] (grid-json request))
  (GET "/admin/cuadrantes/json/form/:id" [id] (form-json id))
  (POST "/admin/cuadrantes/save" request [] (cuadrantes-save request))
  (POST "/admin/cuadrantes/delete" request [] (cuadrantes-delete request)))
