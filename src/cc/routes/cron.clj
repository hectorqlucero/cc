(ns cc.routes.cron
  (:require [cc.models.crud :refer :all]
            [cheshire.core :refer :all]
            [compojure.core :refer :all]
            [selmer.parser :refer [render-file]]))

;; Start crear-ciclistas cron process
(def procesados-sql
  "SELECT
   p.id,
   CONCAT(s.nombre,' ',s.apellido_paterno,' ',s.apellido_materno) as nombre,
   s.direccion,
   s.ciudad,
   s.pais,
   s.celular,
   s.email,
   s2.descripcion as categoria,
   p.puntos
   FROM ciclistas_puntos p
   JOIN ciclistas s on s.id = p.ciclistas_id
   JOIN cartas s1 on s1.id = s.cartas_id
   JOIN categorias s2 on s2.id = s1.categoria
   WHERE
   p.carreras_id = ?
   ORDER BY categoria,nombre")

(def crear-ciclistas-sql
  "SELECT
   nombre,
   apellido_paterno,
   apellido_materno,
   direccion,
   pais,
   ciudad,
   telefono,
   celular,
   dob,
   id AS cartas_id,
   email,
   carreras_id
   FROM cartas
   WHERE carreras_id = ?
   ORDER BY categoria,nombre")

(defn process-cleanup-ciclistas [carreras_id]
  (Delete db :ciclistas ["carreras_id = ?" carreras_id])
  (Delete db :ciclistas_puntos ["carreras_id = ?" carreras_id]))

(defn crear-ciclistas_puntos [carreras_id puntos]
  (let [rows (Query db ["SELECT carreras_id,id AS ciclistas_id FROM ciclistas WHERE carreras_id = ?" carreras_id])
        rows (map #(assoc % :puntos puntos) rows)]
    (Insert-multi db :ciclistas_puntos rows)))

(defn crear-ciclistas [request]
  (let [carreras-row (first (Query db "SELECT * FROM carreras WHERE status='T'"))
        carreras_id (:id carreras-row)
        puntos_p (:puntos_p carreras-row)
        puntos_1 (:puntos_1 carreras-row)
        puntos_2 (:puntos_2 carreras-row)
        puntos_3 (:puntos_3 carreras-row)
        rows (Query db [crear-ciclistas-sql carreras_id])
        crow (first (Query db ["SELECT * fROM carreras WHERE id = ?" carreras_id]))
        carrera (or (:descripcion crow) nil)
        puntos (or (:puntos_p crow) "0")
        result (if (seq rows) (do
                                (process-cleanup-ciclistas carreras_id)
                                (Insert-multi db :ciclistas rows)
                                (crear-ciclistas_puntos carreras_id puntos)) nil)
        prows (Query db [procesados-sql carreras_id])
        prows (map #(assoc % :puntos_1 puntos_1 :puntos_2 puntos_2 :puntos_3 puntos_3 :puntos_p puntos_p) prows)
        title (if (nil? result) "Error: no se crearon ciclistas/puntos!" "Ciclistas y puntos creados apropiadamente!")]
    (render-file "cron/ciclistas_puntos.html" {:title title
                                               :prows prows})))
;; End crear-ciclistas cron process

(defn procesar-puntos [{params :params}]
  (let [id (:id params)
        puntos (:puntos params)
        result (Update db :ciclistas_puntos {:puntos puntos} ["id = ?" id])]
    result))

(defroutes cron-routes
  (GET "/cron/crear/ciclistas" request [] (crear-ciclistas request))
  (POST "/cron/procesar/puntos" request [] (procesar-puntos request)))
