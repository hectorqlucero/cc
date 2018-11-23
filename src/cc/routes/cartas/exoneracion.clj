(ns cc.routes.cartas.exoneracion
  (:require [cc.models.crud :refer :all]
            [cc.models.email :refer [host send-email]]
            [cc.models.grid :refer :all]
            [cc.models.util :refer :all]
            [cheshire.core :refer :all]
            [clj-pdf.core :refer :all]
            [compojure.core :refer :all]
            [ring.util.io :refer :all]
            [selmer.parser :refer [render-file]]))

(defn exoneracion
  []
  (render-file "cartas/exoneracion/index.html" {:title "Cartas - Exoneracion"
                                                   :user  (or (get-session-id) "Anonimo")}))

;;start exoneracion grid
(def search-columns
  ["id"
   "no_participacion"
   "nombre"
   "apellido_paterno"
   "apellido_materno"
   "CASE WHEN sexo = 'M' THEN 'Masculino' WHEN sexo = 'F' THEN 'Femenino' END"
   "telefono"
   "celular"
   "email"
   "CASE WHEN bicicleta = 'F' THEN 'Fija' WHEN bicicleta = 'S' THEN 'SS' ELSE 'Otra' END"])

(def aliases-columns
  ["id"
   "no_participacion"
   "nombre"
   "apellido_paterno"
   "apellido_materno"
   "CASE WHEN sexo = 'M' THEN 'Masculino' WHEN sexo = 'F' THEN 'Femenino' END as sexo"
   "telefono"
   "celular"
   "email"
   "CASE WHEN bicicleta = 'F' THEN 'Fija' WHEN bicicleta = 'S' THEN 'SS' ELSE 'Otra' END as bicicleta"])

(defn grid-json
  [{params :params}]
  (try
    (let [table    "cartas"
          scolumns (convert-search-columns search-columns)
          aliases  aliases-columns
          join     ""
          search   (grid-search (:search params nil) scolumns)
          order    (grid-sort (:sort params nil) (:order params nil))
          offset   (grid-offset (parse-int (:rows params)) (parse-int (:page params)))
          sql      (grid-sql table aliases join search order offset)
          rows     (grid-rows table aliases join search order offset)]
      (generate-string rows))
    (catch Exception e (.getMessage e))))
;;end exoneracion grid

;;start exoneracion form
(def form-sql
  "SELECT *
  FROM cartas
  WHERE id = ?")

(defn form-json
  [id]
  (try
    (let [row (Query db [form-sql id])]
      (generate-string (first row)))
    (catch Exception e (.getMessage e))))
;;end exoneracion form

(defn exoneracion-save
  [{params :params}]
  (try
    (let [id       (fix-id (:id params))
          postvars {:id               id
                    :categoria        (:categoria params)
                    :sexo             (:sexo params)
                    :bicicleta        (:bicicleta params)
                    :no_participacion (:no_participacion params)
                    :nombre           (:nombre params)
                    :apellido_paterno (:apellido_paterno params)
                    :apellido_materno (:apellido_materno params)
                    :equipo           (:equipo params)
                    :direccion        (:direccion params)
                    :pais             (:pais params)
                    :ciudad           (:ciudad params)
                    :telefono         (:telefono params)
                    :celular          (:celular params)
                    :email            (:email params)
                    :tutor            (:tutor params)}
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
  CASE WHEN categoria = 'A' THEN 'Abierta' WHEN categoria = 'N' THEN 'Novatos' END as categoria,
  CASE WHEN sexo = 'V' THEN 'Varonil' WHEN sexo = 'F' THEN 'Femenil' END as sexo,
  CASE WHEN bicicleta = 'F' THEN 'Fija' WHEN bicicleta = 'S' THEN 'SS' WHEN bicicleta = 'O' THEN 'Otra' END as bicicleta,
  no_participacion,
  nombre,
  apellido_paterno,
  apellido_materno,
  equipo,
  direccion,
  pais,
  ciudad,
  telefono,
  celular,
  email,
  tutor
  FROM cartas
  WHERE id = ?")

(defn build-name [row]
  (let [p1 (:nombre row)
        p2 (:apellido_paterno row nil)
        p3 (:apellido_materno row nil)
        nombre-completo (str p1 " " (if-not (nil? p2) p2) " "(if-not (nil? p3) p3))]
    nombre-completo))

(def build-body-p1
  "El que suscribe, por mi propio derecho, expresamente manifiesto que es mi deseo participar en el evento denominado
  \"Circuito Ciclista Navideño Obregon\", que se realizara en la avenida Alvaro Obregón en la ciudad de Mexicali B.C. el
  domingo 2 de Diciembre del año 2018 de 9:00am a 3:00 pm. Así mismo me comprometo y obligo a no ingresar cualquier
  Área RESTRINGIDA(S) (entendida como aquella que requiera la autorización expresa mediante la expedición de
  credencial o permiso por parte del comité organizador, en razón de lo anterior al firmar el presente escrito acepto todos y
  cada uno de los términos y condiciones estipulados en el presente escrito:")


(defn build-body-p2 [row]
  (str "Yo " (build-name row)", Con el número de participación " (:no_participacion row) " por el solo hecho de firmar
este documento, accepto cualquier y todos los riesgos y peligros que sobre mi persona recaigan en cuanto a mi participación
en Evento ciclista denominado \"Circuito Ciclista Navideño Obregon\". Por lo tanto, yo soy el único responsable de mi salud.
cualquier consecuencia, accidentes, perjuicios, deficiencias que puedan causar, de cualquier manera, posibles alteraciones
a mi salud, integridad fisica, o inclusive muerte. Por esa razón libero de cualquier responsabilidad al respecto al Comité
Organizador de dicho Evento y/o a las Asociaciones que lo integran, asi como a sus directores, patrocinadores y
representantes, y por medio de este conducto renuncio, sin limitación alguna a cualquier derecho, demanda o
indemnización al respecto. Reconozco y acepto que Comité Organizador del Evento \"Circuito Ciclista Navideño Obregón\"
no son ni serán consideradas responsables por cualquier desperfecto, pérdida o robo relacionados con mis pertenencias
personales."))

(def build-body-p3
  "También reconozco y acepto que como participante del EVENTO \"Circuito Ciclista Navideño Obregón\", deberé portar en
  todo momento el número de participante proporcionado por los organizadores del EVENTO, en el entendido que dicho
  número no podrán ser transferidas o intercambiadas con cualquier tercero bajo ningún concepto, por lo que si no cuento
  con la misma, los organizadores del EVENTO, podrán retirarme del mismo, leberándolos de toda responsabilidad, asi como
  renunciando a ejercer cualquier acción legal en su contra por las acciones tomadas a este respecto.")

(def build-body-p4
  "Así mismo, autorizo al comité organizador del evento \"Circuito Ciclista Navideño Obregón\", y/o a quien ésta designe, el uso
  de mi imagen y voz, ya sea parcial o totalmente, en cuanto a todo lo relacionado en el Evento, de cualquier manera y en
  cualquier momento. Por este conducto reconozco que sé y entiendo todas las regulaciones del Evento, incluyendo y sin
  limitarse al reglamento de competencia expedido por el Comité Organizador. Igualmente, manifiesto bajo protesta de decir
  verdad que mi equipo de competencia es obligatorio su uso en la competencia (Bicicleta, guantes, casco, zapatos,
  zapatillas, pedales, cadena etc.) los cuales reúnen y cumple con todos los requisitos reglamentarios aplicables, sin
  perjuicios de la facultad que se tenga para revisar su bicicleta y los demás establecidos en lo mencionado en este
  documento.")


(defn build-body [row]
  [:table
   {:cell-border true
    :style :normal
    :align :center
    :width 90
    :size 8
    :border true}
   [[:cell {:colspan 3 :align :center :style :bold} "PARTICIPANTE"]]
   [[:cell {:style :bold} (str "Categoria: " (:categoria row))] [:cell {:style :bold :colspan 2} (str "Bicicleta:" (:bicicleta row))]]
   [[:cell {:style :bold} (str "Nombre completo: " (build-name row))] [:cell {:style :bold} (str "Equipo: " (:equipo row))] [:cell {:style :bold} (str "Numero de Participacion: " (:no_participacion row))]]
   [[:cell {:style :bold} (str "Dirección: " (:direccion row))] [:cell {:style :bold} (str "Pais: " (:pais row))] [:cell {:style :bold} (str "Ciudad: " (:ciudad row))]]
   [[:cell {:style :bold} (str "Telefono: " (:telefono row))] [:cell {:style :bold} (str "Celular: " (:celular row))] [:cell {:style :bold} (str "Email: " (:email row))]]
   [[:cell {:style :bold :colspan 3} (str "Nombre del padre o tutor (En su caso): ")]]
   [[:cell {:style :bold :colspan 3} (str "Firma del participante y/o tutor: ")]]
   ])

(defn execute-report [id]
  (let [h1  "CIRCUITO CICLISTA NAVIDEÑO"
        h2  "OBREGON"
        h3  "CARTA DE EXONERACION"
        row (first (Query db [pdf-sql id]))]
    (piped-input-stream
     (fn [output-stream]
       (pdf
        [{:title         h1
          :header        {:x 20
                          :y 820
                          :table
                          [:pdf-table
                           {:border           false
                            :width-percent    100
                            :horizontal-align :center}
                           [100]
                           [[:pdf-cell {:style :bold :size 12 :leading 13 :align :center} h1]]
                           [[:pdf-cell {:style :bold :size 12 :leading 13 :align :center} h2]]
                           [[:pdf-cell {:style :bold :size 12 :leading 13 :align :center} h3]]]}
          :footer        "page" :left-margin 10
          :right-margin  10
          :top-margin    40
          :bottom-margin 25
          :size          :a4
          :font          {:family :helvetica :size 8}
          :align         :center
          :pages         true}
         [:paragraph {:keep-together false :indent 30} build-body-p1]
         [:spacer]
         [:paragraph {:keep-together false :indent 30} (build-body-p2 row)]
         [:spacer]
         [:paragraph {:keep-together false :indent 30} build-body-p3]
         [:spacer]
         [:paragraph {:keep-together false :indent 30} build-body-p4]
         [:spacer]
         (build-body row)]
        output-stream)))))

(defn exoneracion-pdf [id]
  (let [file-name (str "exoneracion_" id ".pdf")]
    {:headers {"Content-type"        "application/pdf"
               "Content-disposition" (str "attachment;filename=" file-name)}
     :body    (execute-report id)}))
;; End pdf

(defroutes exoneracion-routes
  (GET "/cartas/exoneracion" [] (exoneracion))
  (POST "/cartas/exoneracion/json/grid" request (grid-json request))
  (GET "/cartas/exoneracion/json/form/:id" [id] (form-json id))
  (POST "/cartas/exoneracion/save" request [] (exoneracion-save request))
  (POST "/cartas/exoneracion/delete" request [] (exoneracion-delete request))
  (GET "/cartas/exoneracion/pdf/:id" [id] (exoneracion-pdf id)))
