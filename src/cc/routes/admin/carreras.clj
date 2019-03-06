(ns cc.routes.admin.carreras
  (:require [cc.models.crud :refer :all]
            [cc.models.grid :refer :all]
            [cc.models.util :refer :all]
            [cheshire.core :refer :all]
            [compojure.core :refer :all]
            [selmer.parser :refer [render-file]]))

(defn carreras
  []
  (render-file "admin/carreras/index.html" {:title "Carreras"}))

;;start carreras grid
(def search-columns
  ["id"
   "descripcion"
   "donde"
   "DATE_FORMAT(fecha,'%m/%d/%Y')"
   "TIME_FORMAT(hora,'%h:%i %p')"
   "puntos_p"
   "puntos_1"
   "puntos_2"
   "puntos_3"
   "CASE WHEN status = 'T' THEN 'Activo' ELSE 'Inactivo' END"])

(def aliases-columns
  ["id"
   "descripcion"
   "donde"
   "DATE_FORMAT(fecha,'%m/%d/%Y') as fecha"
   "TIME_FORMAT(hora,'%h:%i %p') as hora"
   "puntos_p"
   "puntos_1"
   "puntos_2"
   "puntos_3"
   "CASE WHEN status = 'T' THEN 'Activo' ELSE 'Inactivo' END as status"])

(defn grid-json
  [{params :params}]
  (try
    (let [table    "carreras"
          scolumns (convert-search-columns search-columns)
          aliases  aliases-columns
          join     ""
          search   (grid-search (:search params nil) scolumns)
          order    (grid-sort (:sort params nil) (:order params nil))
          offset   (grid-offset (parse-int (:rows params)) (parse-int (:page params)))
          rows     (grid-rows table aliases join search order offset)]
      (generate-string rows))
    (catch Exception e (.getMessage e))))
;;end carreras grid

;;start carreras form
(def form-sql
  "SELECT id as id,
   descripcion,
   donde,
   banco,
   banco_cuenta,
   banco_instrucciones,
   organizador,
   DATE_FORMAT(fecha,'%m/%d/%Y') as fecha,
   TIME_FORMAT(hora,'%H:%i') as hora,
   puntos_p,
   puntos_1,
   puntos_2,
   puntos_3,
   status
   FROM carreras
   WHERE id = ?")

(defn form-json
  [id]
  (let [row (Query db [form-sql id])]
    (generate-string (first row))))
;;end cuadrante form

(defn carreras-save
  [{params :params}]
  (let [id       (fix-id (:id params))
        puntos_p (:puntos_p params)
        puntos_1 (:puntos_1 params)
        puntos_2 (:puntos_2 params)
        puntos_3 (:puntos_3 params)
        status   (:status params)
        postvars {:id           id
                  :descripcion (capitalize-words (:descripcion params))
                  :donde (:donde params)
                  :banco (:banco params)
                  :banco_cuenta (:banco_cuenta params)
                  :banco_instrucciones (:banco_instrucciones params)
                  :organizador (:organizador params)
                  :fecha (format-date-internal (:fecha params))
                  :hora (fix-hour (:hora params))
                  :puntos_p puntos_p
                  :puntos_1 puntos_1
                  :puntos_2 puntos_2
                  :puntos_3 puntos_3
                  :status status}
        result   (Save db :carreras postvars ["id = ?" id])
        the-id   (if (nil? id) (get (first result) :generated_key nil) id)]
    (if (seq result)
      (generate-string {:success "Correctamente Processado!"})
      (generate-string {:error "No se pudo processar!"}))))

(defn carreras-delete
  [{params :params}]
  (let [id (:id params nil)
        result (if-not (nil? id)
                 (Delete db :carreras ["id = ?" id])
                 nil)]
    (if (seq result)
      (generate-string {:success "Removido appropiadamente!"})
      (generate-string {:error "No se pudo remover!"}))))

(defroutes carreras-routes
  (GET "/admin/carreras" [] (if-not (= (user-level) "U") (carreras)))
  (POST "/admin/carreras/json/grid" request [] (if-not (= (user-level) "U") (grid-json request)))
  (GET "/admin/carreras/json/form/:id" [id] (if-not (= (user-level) "U") (form-json id)))
  (POST "/admin/carreras/save" request [] (if-not (= (user-level) "U") (carreras-save request)))
  (POST "/admin/carreras/delete" request [] (if-not (= (user-level) "U") (carreras-delete request))))
