(ns cc.routes.talleres
  (:require [cc.models.crud :refer :all]
            [cc.models.grid :refer :all]
            [cc.models.util :refer [fix-id user-level parse-int]]
            [cheshire.core :refer :all]
            [compojure.core :refer :all]
            [clj-pdf.core :refer :all]
            [ring.util.io :refer :all]
            [selmer.parser :refer [render-file]]))

;; Start talleres
(def sql
  "SELECT * FROM taller ORDER BY nombre")

(defn talleres-reporte []
  (let [rows (Query db sql)]
    (render-file "talleres/reportes/index.html" {:title "Talleres de bicicletas"
                                                 :rows rows})))
;; End talleres

;; Start cuadrantes
(def csql
  "SELECT 
   id,
   name,
   leader,
   leader_phone,
   leader_cell,
   leader_email,
   notes,
   CASE WHEN status = 'T' THEN 'Activo' ELSE 'Inactivo' END AS status
   FROM cuadrantes ORDER BY name")

(defn cuadrantes-reporte []
  (let [rows (Query db csql)]
    (render-file "cuadrantes/reportes/index.html" {:title "Talleres de bicicletas"
                                                   :rows rows})))
;; End cuadrantes

(defroutes talleres-routes
  (GET "/talleres/reporte" [] (talleres-reporte))
  (GET "/cuadrantes/reporte" [] (cuadrantes-reporte)))
