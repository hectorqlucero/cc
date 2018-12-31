(ns cc.routes.table_ref
  (:require [cc.models.crud :refer :all]
            [cc.models.util :refer :all]
            [cheshire.core :refer :all]
            [compojure.core :refer :all]))

(def get_users-sql
  "SELECT id AS value, concat(firstname,' ',lastname) AS text FROM users order by firstname,lastname")

(def get_cuadrantes-sql
  "SELECT id AS value, name AS text FROM cuadrantes order by name")

(defn months []
  (list
   {:value 1 :text "Enero"}
   {:value 2 :text "Febrero"}
   {:value 3 :text "Marzo"}
   {:value 4 :text "Abril"}
   {:value 5 :text "Mayo"}
   {:value 6 :text "Junio"}
   {:value 7 :text "Julio"}
   {:value 8 :text "Agosto"}
   {:value 9 :text "Septiembre"}
   {:value 10 :text "Octubre"}
   {:value 11 :text "Noviembre"}
   {:value 12 :text "Diciembre"}))

(defn years [p n]
  (let [year   (parse-int (current_year))
        pyears (for [n (range (parse-int p) 0 -1)] {:value (- year n) :text (- year n)})
        nyears (for [n (range 0 (+ (parse-int n) 1))] {:value (+ year n) :text (+ year n)})
        years  (concat pyears nyears)]
    years))

(defn appointment-options []
  (list
   {:value "T" :text "Pendiente"}
   {:value "X" :text "Remover"}
   {:value "O" :text "Completado en tiempo"}
   {:value "L" :text "Completedo tarde"}
   {:value "E" :text "Completed antes de tiempo"}
   {:value "S" :text "Re-programmado por usuario"}
   {:value "Z" :text "Cancelado por usuario"}))

(defn nivel-options []
  (list
   {:value "P" :text "Principiantes"}
   {:value "M" :text "Medio"}
   {:value "A" :text "Avanzado"}
   {:value "T" :text "TODOS"}))

(defn get-help []
  (str "<a href='/uploads/lucero-systems.pdf'></a>"))

(defn categorias []
  (list
    {:value "A" :text "Infantil Mixta(hasta 14 años"}
    {:value "B" :text "MTB Mixta Montaña"}
    {:value "C" :text "Juveniles Varonil 13-14"}
    {:value "D" :text "Juveniles Varonil 15-17"}
    {:value "E" :text "Novatos Varonil"}
    {:value "F" :text "Master Varonil 40 y mas"}
    {:value "G" :text "Segunda Fuerza"}
    {:value "H" :text "Varonil(intermedios)"}
    {:value "I" :text "Primera Fuerza Varonil (Avanzados"}
    {:value "J" :text "Piñón Fijo Varonil y una velocidad(SS)"}
    {:value "K" :text "Femenil Juvenil 15-17"}
    {:value "L" :text "Segunda Fuerza Femenil(Abierta, Novatas)"}
    {:value "M" :text "Primera Fuerza Femenil(Avanzadas)"}
    {:value "N" :text "Piñón Fijo Femenil y una velocidad(SS)"}))

(defroutes table_ref-routes
  (GET "/table_ref/get_users" [] (generate-string (Query db [get_users-sql])))
  (GET "/table_ref/get_cuadrantes" [] (generate-string (Query db [get_cuadrantes-sql])))
  (GET "/table_ref/months" [] (generate-string (months)))
  (GET "/table_ref/years/:pyears/:nyears" [pyears nyears] (generate-string (years pyears nyears)))
  (GET "/table_ref/appointment_options" [] (generate-string (appointment-options)))
  (GET "/table_ref/nivel_options" [] (generate-string (nivel-options)))
  (GET "/table_ref/help" [] (get-help))
  (GET "/table_ref/categorias" [] (generate-string (categorias))))
