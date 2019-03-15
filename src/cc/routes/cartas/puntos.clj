(ns cc.routes.cartas.puntos
  (:require [cc.models.crud :refer :all]
            [cc.models.grid :refer :all]
            [cc.models.util :refer [fix-id user-level parse-int]]
            [cheshire.core :refer :all]
            [compojure.core :refer :all]
            [clj-pdf.core :refer :all]
            [ring.util.io :refer :all]
            [selmer.parser :refer [render-file]]))

(def carreras_id (atom nil))

(defn puntos []
  (render-file "cartas/carreras/pre_puntos.html" {:title "Actualizar Puntos"}))

;;Start ciclistas_puntos grid
(def search-columns
  ["ciclistas_puntos.id"
   "cartas.no_participacion"
   "cartas.nombre"
   "categorias.descripcion"
   "ciclistas.apellido_paterno"
   "ciclistas.apellido_materno"
   "ciclistas_puntos.puntos_p"
   "ciclistas_puntos.puntos_1"
   "ciclistas_puntos.puntos_2"
   "ciclistas_puntos.puntos_3"])

(def aliases-columns
  ["ciclistas_puntos.id as id"
   "cartas.no_participacion"
   "cartas.nombre"
   "categorias.descripcion as categoria"
   "ciclistas.apellido_paterno"
   "ciclistas.apellido_materno"
   "ciclistas_puntos.puntos_p"
   "ciclistas_puntos.puntos_1"
   "ciclistas_puntos.puntos_2"
   "ciclistas_puntos.puntos_3"])

(defn grid-json [{params :params}]
  (if-not (nil? (:carreras_id params)) (reset! carreras_id (:carreras_id params)))
  (try
    (let [table "ciclistas_puntos"
          scolumns (convert-search-columns search-columns)
          aliases aliases-columns
          join "join ciclistas on ciclistas.id = ciclistas_puntos.ciclistas_id
                join cartas on cartas.id = ciclistas.cartas_id
                join categorias on categorias.id = cartas.categoria"
          search (grid-search (:search params nil) scolumns)
          search (grid-search-extra search (str "ciclistas.carreras_id = " @carreras_id))
          order (grid-sort (:sort params nil) (:order params nil))
          order (grid-sort-extra order "categoria ASC,nombre ASC,apellido_paterno ASC")
          offset (grid-offset (parse-int (:rows params)) (parse-int (:page params)))
          rows (grid-rows table aliases join search order offset)]
      (generate-string rows))
    (catch Exception e (.getMessage e))))
;;End ciclistas_puntos grid

;;Start form
(def form-sql
  "SELECT
   p.id,
   s.nombre,
   s.apellido_paterno,
   s.apellido_materno,
   s1.categoria,
   p.puntos_p,
   p.puntos_1,
   p.puntos_2,
   p.puntos_3
   FROM ciclistas_puntos p
   JOIN ciclistas s on s.id = p.ciclistas_id
   JOIN cartas s1 on s1.id = s.cartas_id
   WHERE p.id = ?")

(defn form-json [id]
  (let [row (Query db [form-sql id])]
    (generate-string (first row))))

(defn puntos-save [{params :params}]
  (try
    (let [id (fix-id (:id params))
          postvars {:id id
                    :puntos_1 (:puntos_1 params)
                    :puntos_2 (:puntos_2 params)
                    :puntos_3 (:puntos_3 params)}
          result (Save db :ciclistas_puntos postvars ["id = ?" id])]
      (if (seq result)
        (generate-string {:success "Correctamente Processado!"})
        (generate-string {:error "No se pudo processar!"})))
    (catch Exception e (.getMessage e))))
;;End form

;;Start puntos grid
(def pdf-sql
  "SELECT
   s1.no_participacion,
   s.nombre,
   s.apellido_paterno,
   s.apellido_materno,
   s2.descripcion as categoria,
   (IFNULL(p.puntos_p,0) + IFNULL(p.puntos_1,0) + IFNULL(p.puntos_2,0) + IFNULL(p.puntos_3,0)) as puntos
   FROM ciclistas_puntos p
   JOIN ciclistas s ON s.id = p.ciclistas_id
   JOIN cartas s1 on s1.id = s.cartas_id
   JOIN categorias s2 on s2.id = s1.categoria
   WHERE p.carreras_id = ?
   ORDER BY s2.descripcion,s.nombre,s.apellido_paterno")

(def report-detail-template
  (template
   (list
    [:cell {:align :left :leading 10} (str $no_participacion)]
    [:cell {:align :left :leading 10} (str $nombre)]
    [:cell {:align :left :leading 10} (str $categoria)]
    [:cell {:align :right :leading 10} (str $puntos)])))

(defn build-body [rows]
  (into
   [:table
    {:cell-border true
     :style :normal
     :size 9
     :border true
     :widths [5 40 40 10]
     :header [{:background-color [233 233 233]}
              [:paragraph {:style :bold :align :left :leading 10} "NUM"]
              [:paragraph {:style :bold :align :left :leading 10} "NOMBRE"]
              [:paragraph {:style :bold :align :left :leading 10} "CATEGORIA"]
              [:paragraph {:style :bold :align :right :leading 10} "PUNTOS"]]}]
   (report-detail-template rows)))

(defn execute-report []
  (let [h1  "SERIAL CICLISTA MEXICALI 2019"
        rows (Query db [pdf-sql @carreras_id])]
    (piped-input-stream
     (fn [output-stream]
       (pdf
        [{:title         h1
          :header        {:x 20
                          :y 790
                          :table
                          [:pdf-table
                           {:border           false
                            :width-percent    100
                            :horizontal-align :center}
                           [100]
                           [[:pdf-cell {:style :bold :size 16 :align :center} h1]]]}
          :footer        "page"
          :top-margin    40
          :bottom-margin 25
          :size          :letter
          :font          {:family :helvetica :size 9}
          :align         :center
          :pages         true}
         (build-body rows)]
        output-stream)))))

(defn puntos-pdf [request]
  (let [carrera-desc (:descripcion (first (Query db "SELECT descripcion from carreras WHERE id = ?" @carreras_id)))
        file-name (str carrera-desc ".pdf")]
    {:headers {"Content-type" "application/pdf"
               "Content-disposition" (str "attachment;filename=" file-name)}
     :body (execute-report)}))
;;END puntos grid

(defn process-puntos [{params :params}]
  (if-not (nil? (:carrera_id params)) (reset! carreras_id (:carrera_id params)))
  (render-file "cartas/carreras/puntos.html" {:title (str "Actualizar Puntos: " (:descripcion (first (Query db ["SELECT descripcion FROM carreras WHERE id = ?" @carreras_id]))))
                                              :carreras_id (:carrera_id params)}))
(defroutes puntos-routes
  (GET "/cartas/puntos" [] (if-not (= (user-level) "U") (puntos)))
  (POST "/cartas/puntos" request [] (if-not (= (user-level) "U") (process-puntos request)))
  (POST "/cartas/puntos/json/grid" request [] (if-not (= (user-level) "U") (grid-json request)))
  (GET "/cartas/puntos/json/form/:id" [id] (if-not (= (user-level) "U") (form-json id)))
  (POST "/cartas/puntos/save" request [] (if-not (= (user-level) "U") (puntos-save request)))
  (GET "/cartas/puntos/pdf" request [] (puntos-pdf request)))
