(ns cc.routes.cartas.categorias
  (:require [cc.models.crud :refer :all]
            [cc.models.grid :refer :all]
            [cc.models.util :refer [parse-int user-level]]
            [cheshire.core :refer :all]
            [compojure.core :refer :all]
            [selmer.parser :refer [render-file]]))

(defn categorias []
  (render-file "cartas/carreras/categorias.html" {:title "Actualizar Categorias"}))

;;Start ciclistas_categorias grid
(def search-columns
  ["id"
   "descripcion"])

(def aliases-columns
  ["id as id"
   "descripcion as descripcion"])

(defn grid-json
  [{params :params}]
  (try
    (let [table "categorias"
          scolumns (convert-search-columns search-columns)
          aliases aliases-columns
          join nil
          search (grid-search (:search params nil) scolumns)
          order (grid-sort (:sort params nil) (:order params nil))
          offset (grid-offset (parse-int (:rows params)) (parse-int (:page params)))
          rows (grid-rows table aliases join search order offset)]
      (generate-string rows))
    (catch Exception e (.getMessage e))))
;;End ciclistas_categorias grid

;;Start form
(def form-sql
  "SELECT
   id,
   descripcion
   FROM categorias
   WHERE id = ?")

(defn form-json [id]
  (let [row (Query db [form-sql id])]
    (generate-string (first row))))

(defn categorias-save [{params :params}]
  (try
    (let [id (clojure.string/upper-case (str (:id params)))
          postvars {:id id
                    :descripcion (:descripcion params)}
          result (Save db :categorias postvars ["id = ?" id])]
      (if (seq result)
        (generate-string {:success "Correctamente Processado!"})
        (generate-string {:error "No se pudo processar!"})))
    (catch Exception e (.getMessage e))))
;;End form

(defroutes categorias-routes
  (GET "/cartas/categorias" [] (if-not (= (user-level) "U") (categorias)))
  (POST "/cartas/categorias/json/grid" request [] (if-not (= (user-level) "U") (grid-json request)))
  (GET "/cartas/categorias/json/form/:id" [id] (if-not (= (user-level) "U") (form-json id)))
  (POST "/cartas/categorias/save" request [] (if-not (= (user-level) "U") (categorias-save request))))
