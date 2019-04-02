(ns cc.routes.contrareloj.crear_carrera
  (:require [cc.models.crud :refer :all]
            [cc.models.util :refer [current_time_internal parse-int]]
            [cheshire.core :refer :all]
            [compojure.core :refer :all]
            [noir.response :refer [redirect]]
            [selmer.parser :refer [render-file]]))

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
   categorias.descripcion as categoria,
   TIME_FORMAT(contrareloj.empezar,'%H:%i:%s') as empezar,
   TIME_FORMAT(contrareloj.terminar,'%H:%i:%s') as terminar
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
(def results-sql
  "SELECT
   p.categorias_id as categorias_id,
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

(defn get-categoria-descripcion [id]
  (:descripcion (first (Query db ["SELECT descripcion FROM categorias WHERE id = ?" id]))))

(defn create-categorias [rows]
  (map (fn [cid]
         {:categorias_id cid
          :categoria (get-categoria-descripcion cid)}) (into '() (into #{} (map #(str (:categorias_id %)) rows)))))

(defn calculate-speed [distance seconds]
  (let [hours (/ (parse-int seconds) 3600.0)
        speed (/ (/ (parse-int distance) 1000) hours)]
    (format "%.3f" speed)))

(defn resultados [request]
  (render-file "contrareloj/pre_resultados.html" {:title "Ver Resultados Todas las Categorias"}))

(defn process [{params :params}]
  (let [carreras_id (:carreras_id params)
        carreras_desc (:descripcion (first (Query db ["SELECT descripcion FROM carreras WHERE id = ?" carreras_id])))
        rows (Query db [results-sql carreras_id])
        crows (create-categorias rows)
        rows (map #(assoc % :speed (str (calculate-speed (:distancia %) (:seconds %)) " km/h")) rows)]
    (render-file "contrareloj/resultados.html" {:title (str "Resultados: " carreras_desc  " (Distancia: " (:distancia (first rows)) " Metros)")
                                                :crows crows
                                                :rows rows})))
;;End show results

;;Start cresultados
(def cresults-sql
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
   AND p.categorias_id = ?
   ORDER BY result")

(defn cresultados [request]
  (render-file "contrareloj/cpre_resultados.html" {:title "Ver Resultados Por Categoria"}))

(defn cprocess [{params :params}]
  (let [carreras_id (:carreras_id params)
        carreras_desc (:descripcion (first (Query db ["SELECT descripcion FROM carreras WHERE id = ?" carreras_id])))
        categorias_id (:categoria params)
        rows (Query db [cresults-sql carreras_id categorias_id])
        rows (map #(assoc % :speed (str (calculate-speed (:distancia %) (:seconds %)) " km/h")) rows)]
    (render-file "contrareloj/c_resultados.html" {:title (str "Resultados/Categoria: " carreras_desc  " (Distancia: " (:distancia (first rows)) " Metros)")
                                                  :rows rows})))
;;End cresultados

;;Start oresultados
(def oresults-sql
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
   ORDER BY result")

(defn oresultados [request]
  (render-file "contrareloj/opre_resultados.html" {:title "Ver Resultados Overall"}))

(defn oprocess [{params :params}]
  (let [carreras_id (:carreras_id params)
        carreras_desc (:descripcion (first (Query db ["SELECT descripcion FROM carreras WHERE id = ?" carreras_id])))
        rows (Query db [oresults-sql carreras_id])
        rows (map #(assoc % :speed (str (calculate-speed (:distancia %) (:seconds %)) " km/h")) rows)]
    (render-file "contrareloj/c_resultados.html" {:title (str "Resultados/Overall: " carreras_desc  " (Distancia: " (:distancia (first rows)) " Metros)")
                                                  :rows rows})))
;;End oresultados

(defroutes crear_carreras-routes
  (GET "/contrareloj" request [] (contra-reloj request))
  (GET "/contrareloj/crear/carrera" request [] (get-carrera request))
  (POST "/contrareloj/crear/carrera" request [] (process-carrera request))
  (GET "/contrareloj/tomar/tiempo" request [] (get-timer request))
  (POST "/contrareloj/tomar/tiempo" request [] (display-timer request))
  (GET "/contrareloj/empezar/tiempo/:id" [id] (empezar-time id))
  (GET "/contrareloj/terminar/tiempo/:id" [id] (terminar-time id))
  (GET "/contrareloj/resultados" request [] (resultados request))
  (POST "/contrareloj/resultados" request [] (process request))
  (GET "/contrareloj/cresultados" request [] (cresultados request))
  (POST "/contrareloj/cresultados" request [] (cprocess request))
  (GET "/contrareloj/oresultados" request [] (oresultados request))
  (POST "/contrareloj/oresultados" request [] (oprocess request)))
