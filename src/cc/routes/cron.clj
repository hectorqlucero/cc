(ns cc.routes.cron
  (:require [cc.models.crud :refer :all]
            [cheshire.core :refer :all]
            [compojure.core :refer :all]))

;; Start process-crear-ciclistas cron process
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
   DATE_FORMAT(dob,'%Y-%m-%d') as dob,
   id AS cartas_id,
   email,
   carreras_id
   FROM cartas
   WHERE carreras_id = ?
   ORDER BY categoria,nombre")

(defn get-active-carrera []
  (first (Query db "SELECT * FROM carreras WHERE status = 'T'")))

(defn process-crear-ciclistas []
  (doseq [row (Query db [crear-ciclistas-sql (:id (get-active-carrera))])]
    (let [id (:id (first (Query db ["SELECT id from ciclistas WHERE email = ? AND carreras_id = ?"
                                    (:email row) (:id (get-active-carrera))])))
          id (str (or id nil))
          postvars {:nombre (str (:nombre row))
                    :apellido_paterno (str (:apellido_paterno row))
                    :apellido_materno (str (:apellido_materno row))
                    :direccion (str (:direccion row))
                    :pais (str (:pais row))
                    :ciudad (str (:ciudad row))
                    :telefono (str (:telefono row))
                    :celular (str (:celular row))
                    :dob (str (:dob row))
                    :cartas_id (str  (:cartas_id row))
                    :id (str id)
                    :email (str (:email row))
                    :carreras_id (str (:carreras_id row))}
          result (Save db :ciclistas postvars ["id = ?" id])]
      result)))
;; End process-crear-ciclistas cron process

(defn process-crear-puntos []
  (let [crow (get-active-carrera)
        carreras_id (:id crow)
        puntos_p (:puntos_p crow)
        rows (Query db ["SELECT * FROM ciclistas WHERE carreras_id = ?" carreras_id])]
    (doseq [row rows]
      (let [ciclistas_id (:id row)
            id (:id
                (first
                 (Query db
                        ["SELECT id from ciclistas_puntos
                            WHERE ciclistas_id = ? AND carreras_id = ?" ciclistas_id carreras_id])))
            id (or id "")
            postvars {:id (str id)
                      :carreras_id (str carreras_id)
                      :ciclistas_id (str ciclistas_id)
                      :puntos_p (str puntos_p)}
            result (Save db :ciclistas_puntos postvars ["id = ?" id])]
        result))))

(defn process-cron [request]
  (process-crear-ciclistas)
  (process-crear-puntos))

(defroutes cron-routes
  (GET "/cron/crear/ciclistas" request [] (process-cron request)))
