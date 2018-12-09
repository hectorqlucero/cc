(ns cc.routes.home
  (:require [cc.models.crud :refer :all]
            [cc.models.util :refer [get-session-id user-level parse-int]]
            [cc.routes.table_ref :refer [months]]
            [cheshire.core :refer :all]
            [compojure.core :refer :all]
            [noir.response :refer [redirect]]
            [noir.session :as session]
            [noir.util.crypt :as crypt]
            [selmer.parser :refer [render-file]]))

;; Get month by month integer
(def column-to-field
  (apply hash-map
         (mapcat
           #(vector (% :value) (% :text))
           (months))))

;;START calendar events
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
  repetir,
  CONCAT('/entrenamiento/rodadas/asistir/',id) as url
  FROM rodadas
  WHERE rodada = 'T'
  ORDER BY fecha,hora ")

(defn purge []
  (Delete db :rodadas ["fecha < CURRENT_DATE() AND repetir != 'T'"]))

(defn repeat-event []
  (let [purge-rows (Query db "SELECT id from rodadas where fecha < CURRENT_DATE()")
        purge-keys (apply str (interpose "," (map #(str (:id %)) purge-rows)))
        sql (str "DELETE from rodadas_link where rodadas_id IN(" purge-keys ")")
        result (if-not (clojure.string/blank? purge-keys)
                 (Query! db sql)
                 nil)]
  (Query! db "UPDATE rodadas SET fecha = DATE_ADD(fecha,INTERVAL 7 DAY) WHERE fecha < CURRENT_DATE()")))

(defn process-confirmados [rodadas_id]
  (let [rows (Query db ["select email from rodadas_link where rodadas_id = ? and asistir = ?" rodadas_id "T"])
        data (if (seq rows)
               (subs (clojure.string/triml (apply str (map #(str ", " (:email %)) rows))) 2)
               "ninguno")]
    data))

(defn main [req]
  (purge)
  (repeat-event)
  (let [rows   (Query db [rodadas-sql])
        rows   (map #(assoc % :confirmados (process-confirmados (:id %))) rows)
        admins (str "<a id=\"btn\" href=\"/login\"><button class='btn btn-info'>Administradores</button></a>")
        help   (str "<a id=\"btn\" href='/uploads/help.pdf' target='_blank'><button class='btn btn-info'>Ayuda: Como usar?</button></a>")
        events (generate-string rows)]
    (render-file "home/main.html" {:title  (str "Calendario de Eventos - Haz clic en el evento para confirmar asistencia  " admins "   " help)
                                   :events events})))
;;END calendar events

(defn eventos [req]
  (purge)
  (repeat-event)
  (let [rows   (Query db [rodadas-sql])
        title  (str "CALENDARIO 2019")
        year   "2019"
        rows   (map #(assoc % :confirmados (process-confirmados (:id %))) rows)
        admins (str "<a id=\"btn\" href=\"/login\"><button class='btn btn-info'>Administradores</button></a>")
        help   (str "<a id=\"btn\" href='/uploads/help.pdf' target='_blank'><button class='btn btn-info'>Ayuda: Como usar?</button></a>")
        events (generate-string rows)]
    (render-file "eventos.html" {:title title
                                 :rows rows
                                 :year year})))
;;END calendar events

(defn login [_]
  (if-not (nil? (get-session-id))
    (redirect "/eventos")
    (render-file "home/login.html" {:title "Accesar el Sito!"})))

(defn login! [username password]
  (let [row    (first (Query db ["select * from users where username = ?" username]))
        active (:active row)]
    (if (= active "T")
      (do
        (if (crypt/compare password (:password row))
          (do
            (session/put! :user_id (:id row))
            (generate-string {:url "/eventos"}))
          (generate-string {:error "Hay problemas para accesar el sitio!"})))
      (generate-string {:error "El usuario esta inactivo!"}))))

(defn user_cleanup [username]
  (Delete db :register ["username =?" username])
  (Delete db :register_link ["register_id =?" username])
  (Delete db :user_link ["user_id = ?" username]))

(defn hql [u p]
  "This is here temporarily to debug and enter a fast encrypt password"
  (let [p (crypt/encrypt p)]
    (Update db :users {:password p} ["username =?" u])))

(defn logoff []
  (session/clear!)
  (redirect "/"))

(defn get-menus []
  (case (user-level)
    "A" (render-file "amenus.html" {})
    "S" (render-file "smenus.html" {})
    "U" (render-file "imenus.html" {})
    (render-file "menus.html" {})))

(defn process-event [request]
  (let [row    (:params request)
        id     (:id row)
        status (:status row)]
    (generate-string (Update db :appointments {:status status} ["id = ?" id]))))

(def eventos-sql
  "
  SELECT
  id,
  DAY(fecha) as day,
  CASE WHEN DAYNAME(fecha) = 'Sunday' THEN 'Domingo' WHEN DAYNAME(fecha) = 'Monday' THEN 'Lunes' WHEN DAYNAME(fecha) = 'Tuesday' THEN 'Martes' WHEN DAYNAME(fecha) = 'Wednesday' THEN 'Miercoles' WHEN DAYNAME(fecha) = 'Thursday' THEN 'Jueves' WHEN DAYNAME(fecha) = 'Friday' THEN 'Viernes' WHEN DAYNAME(fecha) = 'Saturday' THEN 'Sabado' END AS fecha_dow,
  DATE_FORMAT(fecha,'%m/%d/%Y') AS fecha,
  descripcion_corta,
  punto_reunion,
  TIME_FORMAT(hora,'%h:%i %p') as hora,
  leader
  FROM rodadas
  WHERE
  repetir = 'F'
  AND rodada = 'F'
  AND YEAR(fecha) = ?
  AND MONTH(fecha) = ?
  ORDER BY
  DAY(fecha),
  hora ")

(defn display-eventos [year month]
  (let [rows (Query db [eventos-sql year month])]
    (render-file "calendario.html" {:title (column-to-field (parse-int month))
                                    :year year
                                    :rows rows})))

(defroutes home-routes
  (GET "/" request [] (eventos request))
  (GET "/eventos" request [] (eventos request))
  (GET "/eventos/:year/:month" [year month] (display-eventos year month))
  (GET "/login" request [] (login request))
  (POST "/login" [username password] (login! username password))
  (GET "/process_event" request [] (process-event request))
  (GET "/main" request [] (main request))
  (GET "/get_menus" [] (get-menus))
  (GET "/logoff" [] (logoff)))
