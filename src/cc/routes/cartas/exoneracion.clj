(ns cc.routes.cartas.exoneracion
  (:require [cc.models.crud :refer :all]
            [cc.models.email :refer [host send-email]]
            [cc.models.grid :refer :all]
            [cc.models.util :refer :all]
            [cc.models.email :refer [host send-email]]
            [cc.routes.table_ref :refer [categorias]]
            [cheshire.core :refer :all]
            [clj-pdf.core :refer :all]
            [compojure.core :refer :all]
            [ring.util.io :refer :all]
            [selmer.parser :refer [render-file]]))

(def carreras_id (atom nil))

(def totales-sql
  "SELECT
     p.email,
     p.nombre,
     s1.descripcion as categoria,
     SUM((IFNULL(s.puntos_p,0) + IFNULL(s.puntos_1,0) + IFNULL(s.puntos_2,0) + IFNULL(s.puntos_3,0))) as puntos
     FROM cartas p
     JOIN puntos s on s.cartas_id = p.id
     JOIN categorias s1 on s1.id = p.categoria
     GROUP by p.email,p.categoria
     ORDER BY s1.descripcion,puntos DESC,p.nombre")

(def ptotales-sql
  "SELECT
   p.email,
   p.nombre,
   s1.descripcion as categoria,
   SUM((IFNULL(s.puntos_p,0) + IFNULL(s.puntos_1,0) + IFNULL(s.puntos_2,0) + IFNULL(s.puntos_3,0))) as puntos
   FROM cartas p
   JOIN puntos s on s.cartas_id = p.id
   JOIN categorias s1 on s1.id = p.categoria
   GROUP BY p.email,p.categoria
   ORDER BY s1.descripcion,puntos DESC,p.nombre")

(defn carreras-row [] (first (Query db ["SELECT * FROM carreras WHERE id = ?" @carreras_id])))

(defn cartas []
  (let [crow (carreras-row)]
    (render-file "cartas/exoneracion/carta.html" {:title (str (:descripcion crow))
                                                  :fecha (format-date-external (str (:fecha crow)))
                                                  :user (or (get-session-id) "Anonimo")
                                                  :rows (Query db totales-sql)})))

(defn totales []
  (let [crow (carreras-row)]
    (render-file "cartas/exoneracion/totales.html" {:title "Puntuación Total Serial"
                                                    :rows (Query db totales-sql)})))

(defn ptotal []
  (let [crow (carreras-row)]
    (render-file "cartas/exoneracion/ptotal.html" {:title "Puntuación Total Serial"
                                                   :rows (Query db ptotales-sql)})))

(defn exoneracion
  []
  (render-file "cartas/exoneracion/pre_index.html" {:title "Cartas - Exoneracion"
                                                    :user  (or (get-session-id) "Anonimo")}))

(defn exoneracion-processar [{params :params}]
  (if-not (nil? (:carrera_id params)) (reset! carreras_id (:carrera_id params)))
  (render-file "cartas/exoneracion/index.html" {:title (str "Registro de Corredores: " (:descripcion (first (Query db ["SELECT descripcion FROM carreras WHERE id = ?" (:carrera_id params)]))))
                                                :carrera_id @carreras_id}))

;;start exoneracion grid
(def search-columns
  ["cartas.id"
   "cartas.no_participacion"
   "categorias.descripcion"
   "cartas.nombre"
   "cartas.telefono"
   "cartas.email"])

(def aliases-columns
  ["cartas.id"
   "CONCAT(DATE_FORMAT(DATE(cartas.creado),'%m/%d/%Y'),' ',TIME_FORMAT(TIME(cartas.creado),'%h:%i %p')) as creado"
   "cartas.no_participacion"
   "categorias.descripcion as categoria"
   "cartas.nombre"
   "cartas.telefono"
   "cartas.email"])

(defn grid-json
  [{params :params}]
  (if-not (nil? (:carrera_id params)) (reset! carreras_id (:carrera_id params)))
  (try
    (let [table    "cartas"
          scolumns (convert-search-columns search-columns)
          aliases  aliases-columns
          join     "JOIN categorias on categorias.id = cartas.categoria"
          search   (grid-search (:search params nil) scolumns)
          search   (grid-search-extra search (str "cartas.carreras_id = " @carreras_id))
          order    (grid-sort (:sort params nil) (:order params nil))
          offset   (grid-offset (parse-int (:rows params)) (parse-int (:page params)))
          sql      (grid-sql table aliases join search order offset)
          rows     (grid-rows table aliases join search order offset)]
      (generate-string rows))
    (catch Exception e (.getMessage e))))
;;end exoneracion grid

;;start exoneracion form
(def form-sql
  "SELECT
   id,
   no_participacion,
   categoria,
   nombre,
   sexo,
   edad,
   equipo,
   telefono,
   email,
   tutor
   FROM cartas
   WHERE id = ?")

(defn form-json
  [id]
  (try
    (let [row (Query db [form-sql id])]
      (generate-string (first row)))
    (catch Exception e (.getMessage e))))

;;end exoneracion form
(defn get-carreras-desc [carreras_id]
  (:descripcion (first (Query db ["SELECT descripcion FROM carreras WHERE id = ?" carreras_id]))))

(defn get-categorias-desc [c]
  (:descripcion (first (Query db ["SELECT descripcion FROM categorias WHERE id = ?" c]))))

(defn get-email-body [carreras_id nombre email edad telefono equipo categoria]
  {:from "hectorqlucero@gmail.com"
   :to "marcopescador@hotmail.com"
   :cc "hectorqlucero@gmail.com"
   :subject (str "Nuevo Registro para la carrera: " (get-carreras-desc carreras_id))
   :body [{:type "text/html;charset=utf-8"
           :content (str "Nuevo registro: <br>"
                         "<strong>Nombre:</strong> " nombre "<br>"
                         "<strong>Edad:</strong> " edad "<br>"
                         "<strong>Email:</strong> " email "<br>"
                         "<strong>Tel:</strong> " telefono "<br>"
                         "<strong>Equipo:</strong> " equipo "<br>"
                         "<strong>Categoria:</strong> " (get-categorias-desc categoria))}]})

(defn exoneracion-save
  [{params :params}]
  (try
    (let [id (:id params)
          categoria (:categoria params)
          email (:email params)
          carreras_id @carreras_id
          nombre (capitalize-words (:nombre params))
          telefono (:telefono params)
          equipo (clojure.string/upper-case (:equipo params))
          edad (:edad params)
          email-body (get-email-body carreras_id nombre email edad telefono equipo categoria)
          postvars {:id id
                    :no_participacion (:no_participacion params)
                    :categoria categoria
                    :email email
                    :sexo (:sexo params)
                    :edad edad
                    :nombre nombre
                    :equipo equipo
                    :telefono telefono
                    :tutor (capitalize-words (:tutor params))
                    :carreras_id carreras_id}
          result   (Save db :cartas postvars ["id = ?" id])]
      (if (seq result)
        (do
          (send-email host email-body)
          (generate-string {:success "Correctamente Processado!<br>Revise los datos con cuidado antes de cerrar esta pagina. En el encabezado aparece la informacion para realizar su pago. Muchas Gracias!"}))
        (generate-string {:error "No se pudo processar!"})))
    (catch Exception e (.getMessage e))))

(defn exoneracion-delete
  [{params :params}]
  (try
    (let [id     (:id params nil)
          result (if-not (nil? id) (Delete db :cartas ["id = ?" id]))]
      (if (seq result)
        (generate-string {:success "Removido appropiadamente!"})
        (generate-string {:error "No se pudo remover!"})))
    (catch Exception e (.getMessage e))))

;; Start pdf
(def pdf-sql
  "SELECT
  s.descripcion as categoria,
  p.no_participacion,
  p.nombre,
  p.equipo,
  p.telefono,
  p.email,
  p.tutor
  FROM cartas p
  JOIN categorias s on s.id = p.categoria
  WHERE p.id = ?")

(defn build-body-p1 [carreras-row]
  (str "El que suscribe, por mi propio derecho, expresamente manifiesto que es mi deseo participar en el evento denominado
  \"" (:descripcion carreras-row) "\" ," (:donde carreras-row) ". Así mismo me comprometo y obligo a no ingresar cualquier
  Área RESTRINGIDA(S) (entendida como aquella que requiera la autorización expresa mediante la expedición de
  credencial o permiso por parte del comité organizador, en razón de lo anterior al firmar el presente escrito acepto todos y
  cada uno de los términos y condiciones estipulados en el presente escrito:"))

(defn build-body-p2 [row carreras-row]
  (str "Yo " (:nombre row) ", Con el número de participación " (:no_participacion row) " por el solo hecho de firmar
este documento, accepto cualquier y todos los riesgos y peligros que sobre mi persona recaigan en cuanto a mi participación
en Evento ciclista denominado \"" (:descripcion carreras-row) "\". Por lo tanto, yo soy el único responsable de mi salud.
cualquier consecuencia, accidentes, perjuicios, deficiencias que puedan causar, de cualquier manera, posibles alteraciones
a mi salud, integridad fisica, o inclusive muerte. Por esa razón libero de cualquier responsabilidad al respecto al Comité
Organizador de dicho Evento y/o a las Asociaciones que lo integran, asi como a sus directores, patrocinadores y
representantes, y por medio de este conducto renuncio, sin limitación alguna a cualquier derecho, demanda o
indemnización al respecto. Reconozco y acepto que Comité Organizador del Evento \"" (:descripcion carreras-row) \" "
no son ni serán consideradas responsables por cualquier desperfecto, pérdida o robo relacionados con mis pertenencias
personales."))

(defn build-blank-p2 [carreras-row]
  (str "Yo _______________, Con el número de participación _____ por el solo hecho de firmar
este documento, accepto cualquier y todos los riesgos y peligros que sobre mi persona recaigan en cuanto a mi participación
en Evento ciclista denominado \"" (:descripcion carreras-row) "\". Por lo tanto, yo soy el único responsable de mi salud.
cualquier consecuencia, accidentes, perjuicios, deficiencias que puedan causar, de cualquier manera, posibles alteraciones
a mi salud, integridad fisica, o inclusive muerte. Por esa razón libero de cualquier responsabilidad al respecto al Comité
Organizador de dicho Evento y/o a las Asociaciones que lo integran, asi como a sus directores, patrocinadores y
representantes, y por medio de este conducto renuncio, sin limitación alguna a cualquier derecho, demanda o
indemnización al respecto. Reconozco y acepto que Comité Organizador del Evento \"" (:descripcion carreras-row) \" "
no son ni serán consideradas responsables por cualquier desperfecto, pérdida o robo relacionados con mis pertenencias
personales."))

(defn build-body-p3 [carreras-row]
  (str "También reconozco y acepto que como participante del EVENTO \"" (:descripcion carreras-row) "\", deberé portar en
  todo momento el número de participante proporcionado por los organizadores del EVENTO, en el entendido que dicho
  número no podrán ser transferidas o intercambiadas con cualquier tercero bajo ningún concepto, por lo que si no cuento
  con la misma, los organizadores del EVENTO, podrán retirarme del mismo, leberándolos de toda responsabilidad, asi como
  renunciando a ejercer cualquier acción legal en su contra por las acciones tomadas a este respecto."))

(defn build-body-p4 [carreras-row]
  (str "Así mismo, autorizo al comité organizador del evento \"" (:descripcion carreras-row) "\", y/o a quien ésta designe, el uso
  de mi imagen y voz, ya sea parcial o totalmente, en cuanto a todo lo relacionado en el Evento, de cualquier manera y en
  cualquier momento. Por este conducto reconozco que sé y entiendo todas las regulaciones del Evento, incluyendo y sin
  limitarse al reglamento de competencia expedido por el Comité Organizador. Igualmente, manifiesto bajo protesta de decir
  verdad que mi equipo de competencia es obligatorio su uso en la competencia (Bicicleta, guantes, casco, zapatos,
  zapatillas, pedales, cadena etc.) los cuales reúnen y cumple con todos los requisitos reglamentarios aplicables, sin
  perjuicios de la facultad que se tenga para revisar su bicicleta y los demás establecidos en lo mencionado en este
  documento."))

(defn build-body [row]
  [:table
   {:cell-border true
    :style :normal
    :align :center
    :width 90
    :size 8
    :border true}
   [[:cell {:colspan 3 :align :center :style :bold} "PARTICIPANTE"]]
   [[:cell {:style :bold :colspan 3} (str "Categoria: " (:categoria row))]]
   [[:cell {:style :bold} (str "Nombre completo: " (:nombre row))] [:cell {:style :bold} (str "Equipo: " (:equipo row))] [:cell {:style :bold} (str "Numero de Participacion: " (:no_participacion row))]]
   [[:cell {:style :bold} (str "Telefono: " (:telefono row))] [:cell {:style :bold :colspan 2} (str "Email: " (:email row))]]
   [[:cell {:style :bold :colspan 3} (str "Nombre del padre o tutor (En su caso): ")]]
   [[:cell {:style :bold :colspan 3} (str "Firma del participante y/o tutor: ")]]])

(defn execute-report [id]
  (let [h1  "CARTA DE EXONERACION"
        crow (carreras-row)
        row (first (Query db [pdf-sql id]))]
    (piped-input-stream
     (fn [output-stream]
       (pdf
        [{:title         h1
          :references {:logo (or [:image {:align :center :scale 9.5} "uploads/logo.jpg"] nil)}
          :header        {:x 20
                          :y 820
                          :table
                          [:pdf-table
                           {:border           false
                            :width-percent    100
                            :horizontal-align :center}
                           [100]
                           [[:pdf-cell [:reference :logo]]]]}
          :footer        "page" :left-margin 10
          :right-margin  10
          :top-margin    40
          :bottom-margin 25
          :size          :a4
          :font          {:family :helvetica :size 9}
          :align         :center
          :pages         true}
         [:spacer]
         [:spacer]
         [:paragraph {:align :justified :indent-left 28 :indent-right 28} (build-body-p1 crow)]
         [:spacer]
         [:paragraph {:align :justified :indent-left 28 :indent-right 28} (build-body-p2 row crow)]
         [:spacer]
         [:paragraph {:align :justified :indent-left 28 :indent-right 28} (build-body-p3 crow)]
         [:spacer]
         [:paragraph {:align :justified :indent-left 28 :indent-right 28} (build-body-p4 crow)]
         [:spacer]
         (build-body row)]
        output-stream)))))

(defn exoneracion-pdf [id]
  (let [file-name (str "exoneracion_" id ".pdf")]
    {:headers {"Content-type"        "application/pdf"
               "Content-disposition" (str "attachment;filename=" file-name)}
     :body    (execute-report id)}))
;; End pdf

;; Start creporte
(def creporte-template
  (template
   (list
    [:cell {:align :justified} (str $descripcion)]
    [:cell {:align :left} (str $no_participacion)]
    [:cell {:align :left} (str $nombre)]
    [:cell {:align :left} (str $equipo)]
    [:cell {:align :left} "             "])))

(defn carrera-reporte-sql [carreras_id categories]
  (str "SELECT
   categorias.descripcion,
   cartas.no_participacion,
   cartas.nombre,
   cartas.equipo
   FROM cartas
   LEFT JOIN categorias on categorias.id = cartas.categoria
   WHERE
   categoria IN(" categories ")
   AND carreras_id = " carreras_id "
   ORDER BY categorias.descripcion,nombre"))

(defn execute-creporte [carrera_id categorias]
  (let [h1 (:descripcion (first (Query db ["SELECT descripcion FROM carreras WHERE id = ?" carrera_id])))
        h2 "CARRERA POR CATEGORIA(S)"
        categorias (apply str (interpose #"," (map #(str "'" % "'") categorias)))
        rows (Query db (carrera-reporte-sql carrera_id categorias))]
    (piped-input-stream
     (fn [output-stream]
       (pdf
        [{:title         "Reporte de Carrera"
          :header        {:x 20
                          :y 830
                          :table
                          [:pdf-table
                           {:border           false
                            :width-percent    100
                            :horizontal-align :center}
                           [100]
                           [[:pdf-cell {:style :bold :size 16 :align :center} h1]]
                           [[:pdf-cell {:style :bold :size 16 :align :center} h2]]]}
          :footer        "page"
          :left-margin   10
          :right-margin  10
          :top-margin    70
          :bottom-margin 25
          :size          :a4
          :orientation   :portrait
          :font          {:family :helvetica :size 10}
          :align         :center
          :pages         true}
         (into
          [:table
           {:cell-border true
            :style       :normal
            :size        10
            :border      true
            :widths      [25 9 34 20 12]
            :header      [{:backdrop-color [233 233 233]}
                          [:paragraph {:align :justified} "CATEGORIA"]
                          [:paragraph {:align :left} "NUMERO"]
                          [:paragraph {:align :left} "NOMBRE"]
                          [:paragraph {:align :center} "EQUIPO"]
                          [:paragraph {:align :left} "LUGAR"]]}]
          (creporte-template rows))]

        output-stream)))))

(defn creporte-processar [{params :params}]
  (let [file-name (str "Reporte de Carrera")
        carrera_id (:carrera_id params)
        categorias (:categoria_id params)]
    {:headers {"Content-type" "application/pdf"
               "Content-disposition" (str "attachment;filename=" file-name)}}
    :body (execute-creporte carrera_id categorias)))

(defn creporte [{params :params}]
  (render-file "cartas/carreras/carrera.html" {:title "Reporte de Carrera"}))
;; End creporte

;; Start blank pdf
(defn execute-blank-report []
  (let [h1  "CARTA DE EXONERACION"
        row {:categoria nil
             :nombre nil
             :no_participacion nil
             :telefono nil
             :email nil}
        crow (carreras-row)]
    (piped-input-stream
     (fn [output-stream]
       (pdf
        [{:title         h1
          :references {:logo (or [:image {:align :center :scale 9.5} "uploads/logo.jpg"] nil)}
          :header        {:x 20
                          :y 820
                          :table
                          [:pdf-table
                           {:border           false
                            :width-percent    100
                            :horizontal-align :center}
                           [100]
                           [[:pdf-cell [:reference :logo]]]]}
          :footer        "page" :left-margin 10
          :right-margin  10
          :top-margin    40
          :bottom-margin 25
          :size          :a4
          :font          {:family :helvetica :size 9}
          :align         :center
          :pages         true}
         [:spacer]
         [:spacer]
         [:paragraph {:align :justified :indent-left 28 :indent-right 28} (build-body-p1 crow)]
         [:spacer]
         [:paragraph {:align :justified :indent-left 28 :indent-right 28} (build-blank-p2 crow)]
         [:spacer]
         [:paragraph {:align :justified :indent-left 28 :indent-right 28} (build-body-p3 crow)]
         [:spacer]
         [:paragraph {:align :justified :indent-left 28 :indent-right 28} (build-body-p4 crow)]
         [:spacer]
         (build-body row)]
        output-stream)))))

(defn exoneracion-blank-pdf [_]
  (let [file-name (str "exoneracion.pdf")]
    {:headers {"Content-type"        "application/pdf"
               "Content-disposition" (str "attachment;filename=" file-name)}
     :body    (execute-blank-report)}))
;; End blank pdf

(def cartas-sql
  "SELECT
   id,
   categoria,
   no_participacion,
   nombre,
   sexo,
   edad,
   equipo,
   telefono,
   email,
   tutor,
   carreras_id
   FROM cartas
   WHERE email = ?
   AND categoria = ?
   AND carreras_id = ?")

(defn cartas-processar [{params :params}]
  (if-not (nil? (:carreras_id params)) (reset! carreras_id (:carreras_id params)))
  (let [email (:email params)
        categoria (:categoria params)
        carreras-row (first (Query db ["SELECT * FROM carreras WHERE id = ?" @carreras_id]))
        row (first (Query db [cartas-sql email categoria @carreras_id]))
        item {:email email
              :carreras_id @carreras_id
              :categoria categoria
              :banco (str (:banco carreras-row))
              :banco_cuenta (str (:banco_cuenta carreras-row))
              :banco_instrucciones (str (:banco_instrucciones carreras-row))
              :organizador (str (:organizador carreras-row))}
        result (if (seq row) 1 0)
        row (if (seq row) row {:email email
                               :carreras_id @carreras_id
                               :categoria categoria
                               :banco (str (:banco carreras-row))
                               :banco_cuenta (str (:banco_cuenta carreras-row))
                               :banco_instrucciones (str (:banco_instrucciones carreras-row))
                               :organizador (str (:organizador carreras-row))})]
    (render-file "cartas/exoneracion/exoneracion.html" {:title (str (:descripcion carreras-row))
                                                        :user (or (get-session-id) "Anonimo")
                                                        :item item
                                                        :row (generate-string row)
                                                        :exists result})))

(defn slide [request]
  (render-file "cartas/exoneracion/fotos.html" {:title "Carrera Mexicali-Algodones-Mexicali - Marzo 31 2019"}))

(defroutes exoneracion-routes
  (GET "/registro" [] (cartas))
  (GET "/cartas/fotos" request [] (slide request))
  (GET "/cartas/ptotal" [] (totales))
  (GET "/cartas/rtotal" [] (ptotal))
  (GET "/cartas/creporte" request [] (creporte request))
  (POST "/cartas/creporte/processar" request [] (creporte-processar request))
  (POST "/cartas/processar" request [] (cartas-processar request))
  (GET "/cartas/exoneracion" [] (exoneracion))
  (POST "/cartas/exoneracion" request (exoneracion-processar request))
  (POST "/cartas/exoneracion/json/grid" request (grid-json request))
  (GET "/cartas/exoneracion/json/form/:id" [id] (form-json id))
  (POST "/cartas/exoneracion/save" request [] (exoneracion-save request))
  (POST "/cartas/exoneracion/delete" request [] (exoneracion-delete request))
  (GET "/cartas/exoneracion/pdf/:id" [id] (exoneracion-pdf id))
  (GET "/cartas/blank/pdf" request [] (exoneracion-blank-pdf request)))
