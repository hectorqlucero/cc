(ns cc.routes.talleres
  (:require [cc.models.crud :refer :all]
            [cc.models.grid :refer :all]
            [cc.models.util :refer [fix-id user-level parse-int]]
            [cheshire.core :refer :all]
            [compojure.core :refer :all]
            [clj-pdf.core :refer :all]
            [ring.util.io :refer :all]
            [selmer.parser :refer [render-file]]))

(def sql
  "SELECT * FROM taller ORDER BY nombre")
(Query db sql)
(defn talleres-reporte []
  (let [rows (Query db sql)]
    (render-file "talleres/reportes/index.html" {:title "Talleres de bicicletas"
                                                 :rows rows})))

(defroutes talleres-routes
  (GET "/talleres/reporte" [] (talleres-reporte)))
