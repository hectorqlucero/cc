(ns cc.routes.cartas.exoneracion
  (:require [cc.models.crud :refer :all]
            [cc.models.email :refer [host send-email]]
            [cc.models.grid :refer :all]
            [cc.models.util :refer :all]
            [cc.routes.table_ref :refer [categorias]]
            [cheshire.core :refer :all]
            [clj-pdf.core :refer :all]
            [compojure.core :refer :all]
            [ring.util.io :refer :all]
            [selmer.parser :refer [render-file]]))

(defn carreras-row [] (first (Query db "SELECT * FROM carreras WHERE status = 'T'")))

(defn cartas []
  (render-file "cartas/exoneracion/carta.html" {:title "Carta - Exoneracion"
                                                :user (or (get-session-id) "Anonimo")}))

(defn exoneracion
  []
  (render-file "cartas/exoneracion/index.html" {:title "Cartas - Exoneracion"
                                                :user  (or (get-session-id) "Anonimo")}))

;;start exoneracion grid
(def search-columns
  ["cartas.id"
   "CONCAT(DATE_FORMAT(DATE(cartas.creado),'%m/%d/%Y'),' ',TIME_FORMAT(TIME(cartas.creado),'%h:%i %p'))"
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
  (try
    (let [table    "cartas"
          carreras_id (:id (carreras-row))
          scolumns (convert-search-columns search-columns)
          aliases  aliases-columns
          join     "JOIN categorias on categorias.id = cartas.categoria"
          search   (grid-search (:search params nil) scolumns)
          search   (grid-search-extra search (str "carreras_id = " carreras_id))
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
(defn get-categorias-desc [c]
  (:descripcion (first (Query db ["SELECT descripcion FROM categorias WHERE id = ?" c]))))

(defn exoneracion-save
  [{params :params}]
  (try
    (let [id (or (:id params) "")
          carreras_id (str (:id (carreras-row)))
          postvars {:id id
                    :no_participacion (:no_participacion params)
                    :categoria (:categoria params)
                    :email (:email params)
                    :nombre (capitalize-words (:nombre params))
                    :equipo (:equipo params)
                    :telefono (:telefono params)
                    :tutor (capitalize-words (:tutor params))
                    :carreras_id carreras_id}
          result   (Save db :cartas postvars ["id = ?" id])]
      (if (seq result)
        (generate-string {:success "Correctamente Processado!"})
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
indemnización al respecto. Reconozco y acepto que Comité Organizador del Evento \"Serial Ciclista de Mexicali 2019\"
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
indemnización al respecto. Reconozco y acepto que Comité Organizador del Evento \"Serial Ciclista de Mexicali 2019\"
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
   equipo,
   telefono,
   email,
   tutor,
   carreras_id
   FROM cartas
   WHERE email = ?
   AND categoria = ?")

(defn cartas-processar [{params :params}]
  (let [email (:email params)
        categoria (:categoria params)
        row (first (Query db [cartas-sql email categoria]))
        result (if (seq row) 1 0)
        row (if (seq row) row {:email email
                               :categoria categoria})]
    (render-file "cartas/exoneracion/exoneracion.html" {:title "Registro Serial Ciclista Mexicali"
                                                        :user (or (get-session-id) "Anonimo")
                                                        :row (generate-string row)
                                                        :exists result})))

(defroutes exoneracion-routes
  (GET "/registro" [] (cartas))
  (POST "/cartas/processar" request [] (cartas-processar request))
  (GET "/cartas/exoneracion" [] (exoneracion))
  (POST "/cartas/exoneracion/json/grid" request (grid-json request))
  (GET "/cartas/exoneracion/json/form/:id" [id] (form-json id))
  (POST "/cartas/exoneracion/save" request [] (exoneracion-save request))
  (POST "/cartas/exoneracion/delete" request [] (exoneracion-delete request))
  (GET "/cartas/exoneracion/pdf/:id" [id] (exoneracion-pdf id))
  (GET "/cartas/blank/pdf" request [] (exoneracion-blank-pdf request)))
