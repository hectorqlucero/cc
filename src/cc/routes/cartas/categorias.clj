(ns cc.routes.cartas.categorias
  (:require [cc.models.crud :refer :all]
            [cc.models.grid :refer :all]
            [cc.models.util :refer [fix-id user-level parse-int]]
            [cheshire.core :refer :all]
            [compojure.core :refer :all]
            [clj-pdf.core :refer :all]
            [ring.util.io :refer :all]
            [selmer.parser :refer [render-file]]))

(defn categorias []
  (render-file "cartas/carreras/categorias.html" {:title "Actualizar Categorias"}))

;;Start ciclistas_categorias grid
(def search-columns
  ["ciclistas_categorias.id"
   "cartas.no_participacion"
   "ciclistas.nombre"
   "categorias.descripcion"
   "ciclistas.apellido_paterno"
   "ciclistas.apellido_materno"
   "ciclistas_categorias.puntos_p"
   "ciclistas_categorias.puntos_1"
   "ciclistas_categorias.puntos_2"
   "ciclistas_categorias.puntos_3"])

(def aliases-columns
  ["ciclistas_categorias.id as id"
   "cartas.no_participacion"
   "ciclistas.nombre"
   "categorias.descripcion as categoria"
   "ciclistas.apellido_paterno"
   "ciclistas.apellido_materno"
   "ciclistas_categorias.puntos_p"
   "ciclistas_categorias.puntos_1"
   "ciclistas_categorias.puntos_2"
   "ciclistas_categorias.puntos_3"])

(defn grid-json
  [{params :params}]
  (try
    (let [table "ciclistas_categorias"
          carreras_id (:id (first (Query db "SELECT id from carreras where status = 'T'")))
          scolumns (convert-search-columns search-columns)
          aliases aliases-columns
          join "join ciclistas on ciclistas.id = ciclistas_categorias.ciclistas_id
                join cartas on cartas.id = ciclistas.cartas_id
                join categorias on categorias.id = cartas.categoria"
          search (grid-search (:search params nil) scolumns)
          search (grid-search-extra search (str "ciclistas.carreras_id = " carreras_id))
          order (grid-sort (:sort params nil) (:order params nil))
          order (grid-sort-extra order "categoria ASC,nombre ASC,apellido_paterno ASC")
          offset (grid-offset (parse-int (:rows params)) (parse-int (:page params)))
          rows (grid-rows table aliases join search order offset)]
      (generate-string rows))
    (catch Exception e (.getMessage e))))
;;End ciclistas_categorias grid

;;Start form
(def form-sql
  "SELECT
   p.id,
   s.nombre,
   s.apellido_paterno,
   s.apellido_materno,
   s1.categoria,
   p.categorias_p,
   p.categorias_1,
   p.categorias_2,
   p.categorias_3
   FROM ciclistas_categorias p
   JOIN ciclistas s on s.id = p.ciclistas_id
   JOIN cartas s1 on s1.id = s.cartas_id
   WHERE p.id = ?")

(defn form-json [id]
  (let [row (Query db [form-sql id])]
    (generate-string (first row))))

(defn categorias-save [{params :params}]
  (try
    (let [id (fix-id (:id params))
          postvars {:id id
                    :categorias_1 (:puntos_1 params)
                    :categorias_2 (:puntos_2 params)
                    :categorias_3 (:puntos_3 params)}
          result (Save db :ciclistas_categorias postvars ["id = ?" id])]
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
