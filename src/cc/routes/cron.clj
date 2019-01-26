(ns cc.routes.cron
  (:require [cc.models.crud :refer :all]
            [cheshire.core :refer :all]
            [noir.response :refer [redirect]]
            [compojure.core :refer :all]))

;; Start process-crear-ciclistas cron process
(def crear-ciclistas-sql
  "SELECT
   no_participacion,
   nombre,
   telefono,
   id AS cartas_id,
   email,
   categoria,
   carreras_id
   FROM cartas
   WHERE carreras_id = ?
   ORDER BY categoria,nombre")

(defn get-active-carrera []
  (first (Query db "SELECT * FROM carreras WHERE status = 'T'")))

(defn process-crear-ciclistas [_]
  (doseq [row (Query db [crear-ciclistas-sql (:id (get-active-carrera))])]
    (let [id (:id (first (Query db ["SELECT id from ciclistas WHERE no_participacion = ? AND carreras_id = ?"
                                    (:no_participacion row) (:id (get-active-carrera))])))
          id (str (or id nil))
          postvars {:nombre (str (:nombre row))
                    :no_participacion (str (:no_participacion row))
                    :telefono (str (:telefono row))
                    :dob (str (:dob row))
                    :categoria (str (:categoria row))
                    :cartas_id (str  (:cartas_id row))
                    :id (str id)
                    :email (str (:email row))
                    :carreras_id (str (:carreras_id row))}
          result (Save db :ciclistas postvars ["id = ?" id])])))
;; End process-crear-ciclistas cron process

(defn process-crear-puntos [_]
  (let [crow (get-active-carrera)
        carreras_id (:id crow)
        puntos_p (:puntos_p crow)
        rows (Query db ["SELECT * FROM ciclistas WHERE carreras_id = ?" carreras_id])]
    (doseq [row rows]
      (let [ciclistas_id (:id row)
            no_participacion (:no_participacion row)
            categoria (:categoria row)
            id (:id
                (first
                 (Query db
                        ["SELECT id from ciclistas_puntos
                            WHERE ciclistas_id = ? AND carreras_id = ? AND no_participacion = ?" ciclistas_id carreras_id no_participacion])))
            id (or id nil)
            postvars {:id (str id)
                      :no_participacion (str no_participacion)
                      :carreras_id (str carreras_id)
                      :categoria (str categoria)
                      :ciclistas_id (str ciclistas_id)
                      :puntos_p (str puntos_p)}
            result (Save db :ciclistas_puntos postvars ["id = ?" id])]))))

(defn process-cron [request]
  (process-crear-ciclistas request)
  (process-crear-puntos request)
  (redirect "/cartas/puntos"))

(defroutes cron-routes
  (GET "/cron/crear/ciclistas" request [] (process-cron request)))
