(ns cc.routes.calendario.imdecuf
  (:require [cc.models.crud :refer :all]
            [cc.models.grid :refer :all]
            [cc.models.util :refer :all]
            [cc.routes.table_ref :refer [months]]
            [cheshire.core :refer :all]
            [compojure.core :refer :all]
            [selmer.parser :refer [render-file]]))

;; Get month by month integer
(def column-to-field
  (apply hash-map
         (mapcat
           #(vector (% :value) (% :text))
           (months))))

(def eventos-sql
  "
  SELECT
  id,
  DAY(fecha) as day,
  CASE WHEN DAYNAME(fecha) = 'Sunday' THEN 'Domingo' WHEN DAYNAME(fecha) = 'Monday' THEN 'Lunes' WHEN DAYNAME(fecha) = 'Tuesday' THEN 'Martes' WHEN DAYNAME(fecha) = 'Wednesday' THEN 'Miercoles' WHEN DAYNAME(fecha) = 'Thursday' THEN 'Jueves' WHEN DAYNAME(fecha) = 'Friday' THEN 'Viernes' WHEN DAYNAME(fecha) = 'Saturday' THEN 'Sabado' END AS fecha_dow,
  DATE_FORMAT(fecha,'%m/%d/%Y') AS fecha,
  descripcion,
  descripcion_corta,
  punto_reunion,
  TIME_FORMAT(hora,'%h:%i %p') as hora,
  leader
  FROM imdecuf
  WHERE
  repetir = 'F'
  AND rodada = 'F'
  AND YEAR(fecha) = ?
  AND MONTH(fecha) = ?
  ORDER BY
  DAY(fecha),
  hora ")

(def rodadas-sql
  "SELECT
  id,
  descripcion_corta as title,
  descripcion as description,
  CONCAT(fecha,'T',hora) as start,
  punto_reunion as donde,
  CASE WHEN nivel = 'P' THEN 'Principiantes' WHEN nivel = 'M' THEN 'Medio' WHEN nivel = 'A' THEN 'Avanzado' WHEN nivel = 'T' THEN 'TODOS' END as nivel,
  distancia as distancia,
  velocidad as velocidad,
  leader as leader,
  leader_email as email,
  rodada as rodada,
  repetir
  FROM imdecuf
  WHERE rodada = 'T'
  ORDER BY fecha,hora ")

(defn eventos [req]
  (let [rows   (Query db [rodadas-sql])
        title  (str "CALENDARIO 2019")
        year   "2019"
        rows   rows
        events (generate-string rows)]
    (render-file "eventos_imdecuf.html" {:title title
                                 :rows rows
                                 :year year})))

(defn display-eventos [year month]
  (let [rows (Query db [eventos-sql year month])
        rows (map #(assoc % :day (zpl (% :day) 2)) rows)]
    (render-file "calendario_imdecuf.html" {:title (column-to-field (parse-int month))
                                    :year year
                                    :month month
                                    :rows rows})))

(defn calendario-eventos
  [request]
  (render-file "calendario/eventos/imdecuf.html" {:title "Calendario - Eventos"
                                                   :user "Anonimo"}))
;;start eventos grid
(def search-columns
  ["id"
   "descripcion_corta"
   "descripcion"
   "punto_reunion"
   "CASE WHEN repetir = 'T' THEN 'Si' ELSE 'No' END"
   "DATE_FORMAT(fecha,'%m/%d/%Y')"
   "TIME_FORMAT(hora,'%h:%i %p')"
   "leader"])

(def aliases-columns
  ["id"
   "descripcion_corta"
   "descripcion"
   "punto_reunion"
   "CASE WHEN repetir = 'T' THEN 'Si' ELSE 'No' END as repetir"
   "DATE_FORMAT(fecha,'%m/%d/%Y') as fecha"
   "TIME_FORMAT(hora,'%h:%i %p') as hora"
   "leader"])

(defn grid-json
  [{params :params}]
  (try
    (let [table    "imdecuf"
          scolumns (convert-search-columns search-columns)
          aliases  aliases-columns
          join     ""
          search   (grid-search (:search params nil) scolumns)
          search   (grid-search-extra search "rodada = 'F'")
          order    (grid-sort (:sort params nil) (:order params nil))
          offset   (grid-offset (parse-int (:rows params)) (parse-int (:page params)))
          rows     (grid-rows table aliases join search order offset)]
      (generate-string rows))
    (catch Exception e (.getMessage e))))
;;end eventos grid

;;start eventos form
(def form-sql
  "SELECT id as id,
  descripcion,
  descripcion_corta,
  punto_reunion,
  DATE_FORMAT(fecha,'%m/%d/%Y') as fecha,
  TIME_FORMAT(hora,'%H:%i') as hora,
  leader,
  leader_email,
  repetir,
  anonimo
  FROM imdecuf
  WHERE id = ?")

(defn form-json
  [id]
  (try
    (let [row (Query db [form-sql id])]
      (generate-string (first row)))
    (catch Exception e (.getMessage e))))
;;end eventos form

(defn eventos-save
  [{params :params}]
  (try
    (let [id       (fix-id (:id params))
          postvars {:id                id
                    :descripcion       (:descripcion params)
                    :descripcion_corta (:descripcion_corta params)
                    :punto_reunion     (:punto_reunion params)
                    :fecha             (format-date-internal (:fecha params))
                    :hora              (fix-hour (:hora params))
                    :leader            (:leader params)
                    :leader_email      (:leader_email params)
                    :cuadrante         (:cuadrante params)
                    :repetir           "F"
                    :rodada            "F"
                    :anonimo           "T"}
          result   (Save db :imdecuf postvars ["id = ?" id])]
      (if (seq result)
        (generate-string {:success "Correctamente Processado!"})
        (generate-string {:error "No se pudo processar!"})))
    (catch Exception e (.getMessage e))))

;;Start eventos-delete
(defn eventos-delete
  [{params :params}]
  (try
    (let [id     (:id params nil)
          result (if-not (nil? id)
                   (Delete db :imdecuf ["id = ?" id])
                   nil)]
      (if (seq result)
        (generate-string {:success "Removido appropiadamente!"})
        (generate-string {:error "No se pudo remover!"})))
    (catch Exception e (.getMessage e))))
;;End eventos-delete

(defroutes imdecuf-routes
  (GET "/imdecuf" request [] (eventos request))
  (GET "/imdecuf/display/:year/:month" [year month] (display-eventos year month))
  (GET "/imdecuf/calendario" request [] (calendario-eventos request))
  (POST "/imdecuf/calendario/json/grid" request (grid-json request))
  (GET "/imdecuf/calendario/json/form/:id" [id] (form-json id))
  (POST "/imdecuf/calendario/save" request [] (eventos-save request))
  (POST "/imdecuf/calendario/delete" request [] (eventos-delete request)))

