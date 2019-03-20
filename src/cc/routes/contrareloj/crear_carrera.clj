(ns cc.routes.contrareloj.crear_carrera
  (:require [cc.models.crud :refer :all]
            [cc.models.util :refer [current_time_internal]]
            [cheshire.core :refer :all]
            [noir.response :refer [redirect]]
            [selmer.parser :refer [render-file]]
            [compojure.core :refer :all]))

(defn contra-reloj [_]
  (render-file "contrareloj/index.html" {:title "Carreras Contra Reloj"}))

;;Start crear carrera
(defn get-carrera [request]
  (render-file "contrareloj/pre_carrera.html" {:title "Crear Carrera Contra Reloj"}))

(def crear_carrera-sql
  "SELECT
   id,
   carreras_id,
   categoria
   FROM cartas
   WHERE carreras_id = ?
   ORDER BY no_participacion ")

(defn process-carrera [{params :params}]
  (let [carreras_id (:carreras_id params)
        crow (first (Query db ["SELECT * FROM carreras WHERE id = ?" carreras_id]))
        rows (Query db [crear_carrera-sql carreras_id])]
    (doseq [row rows]
      (let [cartas_id (str (:id row))
            carreras_id (str (:carreras_id row))
            categorias_id (str (:categoria row))
            id (:id
                (first
                 (Query db
                        ["SELECT id from contrareloj
                            WHERE cartas_id = ? AND carreras_id = ? AND categorias_id = ?" cartas_id carreras_id categorias_id])))
            postvars {:id (str id)
                      :cartas_id cartas_id
                      :carreras_id carreras_id
                      :categorias_id categorias_id}
            result (Save db :contrareloj postvars ["id = ?" id])])))
  (redirect "/contrareloj"))

;;End crear carrera

;;Start tomar tiempo
(defn get-timer [request]
  (render-file "contrareloj/pre_timer.html" {:title "Tomar Tiempo"}))

(def timer-sql
  "SELECT
   contrareloj.id as id,
   cartas.no_participacion as numero,
   cartas.nombre as nombre,
   categorias.descripcion as categoria
   FROM contrareloj
   JOIN cartas on cartas.id = contrareloj.cartas_id
   LEFT join categorias on categorias.id = contrareloj.categorias_id
   WHERE contrareloj.carreras_id = ?
   ORDER BY cartas.no_participacion,cartas.nombre,categorias.descripcion")

(defn display-timer [{params :params}]
  (let [carreras_id (:carreras_id params)
        carrera_desc (:descripcion (first (Query db ["SELECT descripcion FROM carreras where id = ?" carreras_id])))
        rows (Query db [timer-sql carreras_id])]
    (render-file "contrareloj/timer.html" {:title (str "Tomar Tiempo para: " carrera_desc)
                                           :rows rows})))
;;End tomar tiempo

(defn empezar-time [id]
  (let [current-time (current_time_internal)
        result (Update db :contrareloj {:empezar current-time} ["id = ?" id])]
    (if (seq result)
      (generate-string {:time (str current-time)})
      (generate-string {:time "Not able to generate time!"}))))

(defn terminar-time [id]
  (let [current-time (current_time_internal)
        result (Update db :contrareloj {:terminar current-time} ["id = ?" id])]
    (if (seq result)
      (generate-string {:time (str current-time)})
      (generate-string {:time "Not able to generate time!"}))))

;;Start show results
(def carreras_categorias-sql
  "SELECT
   p.categorias_id as categorias_id,
   s.descripcion as categoria
   FROM carreras_categorias p
   JOIN categorias s on s.id = p.categorias_id
   WHERE p.carreras_id = ?")

(def results-sql
  "SELECT
   s0.descripcion as categoria,
   p.carreras_id as carreras_id,
   s1.distancia as distancia,
   s.no_participacion as numero,
   s.nombre as nombre,
   TIME_FORMAT(p.empezar, '%H:%i:%s') as empezar,
   TIME_FORMAT(p.terminar, '%H:%i:%s') as terminar,
   TIME_FORMAT(SUBTIME(p.terminar,p.empezar),'%H:%i:%s') as result,
   TIME_TO_SEC(SUBTIME(p.terminar,p.empezar)) as seconds
   FROM contrareloj p
   JOIN cartas s on s.id = p.cartas_id
   JOIN categorias s0 on s0.id = p.categorias_id
   JOIN carreras s1 on s1.id = p.carreras_id
   WHERE p.empezar IS NOT NULL
   AND p.terminar IS NOT NULL
   AND p.carreras_id = ?
   ORDER BY s0.descripcion,result")

(defn calculate-speed [distance seconds]
  (let [hours (/ seconds (* 60.0 60.0))
        speed (/ distance hours)]
    (format "%.3f" speed)))

(defn process [{params :params}]
  (let [carreras_id (:carreras_id params)
        carreras_desc (:descripcion (first (Query db ["SELECT descripcion FROM carreras WHERE id = ?" carreras_id])))
        rows (Query db [results-sql carreras_id])
        rows (map #(assoc % :speed (str (calculate-speed (:distancia %) (:seconds %)) " km/h")) rows)]
    (render-file "contrareloj/resultados.html" {:title (str "Resultados: " carreras_desc  " (Distancia: " (:distancia (first rows)) " Kilometros)")
                                                :rows rows})))

(defn resultados [request]
  (render-file "contrareloj/pre_resultados.html" {:title "Ver Resultados"}))
;;End show results

(defroutes crear_carreras-routes
  (GET "/contrareloj" request [] (contra-reloj request))
  (GET "/contrareloj/crear/carrera" request [] (get-carrera request))
  (POST "/contrareloj/crear/carrera" request [] (process-carrera request))
  (GET "/contrareloj/tomar/tiempo" request [] (get-timer request))
  (POST "/contrareloj/tomar/tiempo" request [] (display-timer request))
  (GET "/contrareloj/empezar/tiempo/:id" [id] (empezar-time id))
  (GET "/contrareloj/terminar/tiempo/:id" [id] (terminar-time id))
  (GET "/contrareloj/resultados" request [] (resultados request))
  (POST "/contrareloj/resultados" request [] (process request)))
