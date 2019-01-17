(ns cc.routes.cartas.puntos
  (:require [cc.models.crud :refer :all]
            [cc.models.grid :refer :all]
            [cc.models.util :refer [fix-id user-level parse-int]]
            [cheshire.core :refer :all]
            [compojure.core :refer :all]
            [selmer.parser :refer [render-file]]))

(defn puntos []
  (render-file "cartas/carreras/puntos.html" {:title "Actualizar Puntos"}))

;;Start ciclistas_puntos grid
(def search-columns
  ["ciclistas.id"
   "ciclistas.nombre"
   "categorias.descripcion"
   "ciclistas.apellido_paterno"
   "ciclistas.apellido_materno"
   "ciclistas_puntos.puntos_p"
   "ciclistas_puntos.puntos_1"
   "ciclistas_puntos.puntos_2"
   "ciclistas_puntos.puntos_3"])

(def aliases-columns
  ["ciclistas.id as id"
   "ciclistas.nombre"
   "categorias.descripcion as categoria"
   "ciclistas.apellido_paterno"
   "ciclistas.apellido_materno"
   "ciclistas_puntos.puntos_p"
   "ciclistas_puntos.puntos_1"
   "ciclistas_puntos.puntos_2"
   "ciclistas_puntos.puntos_3"])

(defn grid-json
  [{params :params}]
  (try
    (let [table    "ciclistas_puntos"
          scolumns (convert-search-columns search-columns)
          aliases  aliases-columns
          join     "join ciclistas on ciclistas.id = ciclistas_puntos.ciclistas_id
                    join cartas on cartas.id = ciclistas.cartas_id
                    join categorias on categorias.id = cartas.categoria"
          search   (grid-search (:search params nil) scolumns)
          order    (grid-sort (:sort params nil) (:order params nil))
          order    (grid-sort-extra order "categoria ASC,nombre ASC,apellido_paterno ASC")
          offset   (grid-offset (parse-int (:rows params)) (parse-int (:page params)))
          rows     (grid-rows table aliases join search order offset)]
      (generate-string rows))
    (catch Exception e (.getMessage e))))
;;End ciclistas_puntos grid

;;Start form
(def form-sql
  "SELECT
   p.id,
   s.nombre,
   s.apellido_paterno,
   s.apellido_materno,
   s1.categoria,
   p.puntos_p,
   p.puntos_1,
   p.puntos_2,
   p.puntos_3
   FROM ciclistas_puntos p
   JOIN ciclistas s on s.id = p.ciclistas_id
   JOIN cartas s1 on s1.id = s.cartas_id
   WHERE p.id = ?")

(defn form-json [id]
  (let [row (Query db [form-sql id])]
    (generate-string (first row))))

(defn puntos-save [{params :params}]
  (try
    (let [id (fix-id (:id params))
          postvars {:id id
                    :puntos_1 (:puntos_1 params)
                    :puntos_2 (:puntos_2 params)
                    :puntos_3 (:puntos_3 params)}
          result (Save db :ciclistas_puntos postvars ["id = ?" id])]
      (if (seq result)
        (generate-string {:success "Correctamente Processado!"})
        (generate-string {:error "No se pudo processar!"})))
    (catch Exception e (.getMessage e))))
;;End form

(defroutes puntos-routes
  (GET "/cartas/puntos" [] (if-not (= (user-level) "U") (puntos)))
  (POST "/cartas/puntos/json/grid" request [] (if-not (= (user-level) "U") (grid-json request)))
  (GET "/cartas/puntos/json/form/:id" [id] (if-not (= (user-level) "U") (form-json id)))
  (POST "/cartas/puntos/save" request [] (if-not (= (user-level) "U") (puntos-save request))))
