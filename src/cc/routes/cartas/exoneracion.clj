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
            [selmer.parser :refer [render-file]])
  (:import (java.time LocalDate
                      format.DateTimeFormatter
                      Year
                      temporal.ChronoUnit)))

(defn age [dob]
  (let [date-formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd")
        dob (LocalDate/parse dob date-formatter)
        now (LocalDate/now)
        age (.until dob now ChronoUnit/YEARS)]
    age))

(defn cartas []
  (render-file "cartas/exoneracion/carta.html" {:title "Carta - Exoneracion"
                                                :user (or (get-session-id) "Anonimo")}))

(defn exoneracion
  []
  (render-file "cartas/exoneracion/index.html" {:title "Cartas - Exoneracion"
                                                :user  (or (get-session-id) "Anonimo")}))

;;start exoneracion grid
(def search-columns
  ["id"
   "creado"
   "no_participacion"
   "nombre"
   "apellido_paterno"
   "apellido_materno"
   "CASE WHEN sexo = 'M' THEN 'Masculino'
    WHEN sexo = 'F' THEN 'Femenino' END as sexo"
   "telefono"
   "celular"
   "email"
   "CASE WHEN bicicleta='R' THEN 'Bicicleta de ruta'
    WHEN bicicleta='M' THEN 'Bicicleta de montaña'
    WHEN bicicleta='F' THEN 'Bicicleta fija/SS'
    WHEN bicicleta='O' THEN 'Otra' END as bicicleta"])

(def aliases-columns
  ["id"
   "creado"
   "no_participacion"
   "nombre"
   "apellido_paterno"
   "apellido_materno"
   "CASE WHEN sexo = 'M' THEN 'Masculino'
    WHEN sexo = 'F' THEN 'Femenino' END as sexo"
   "telefono"
   "celular"
   "email"
   "CASE WHEN bicicleta='R' THEN 'Bicicleta de ruta'
    WHEN bicicleta='M' THEN 'Bicicleta de montaña'
    WHEN bicicleta='F' THEN 'Bicicleta fija/SS'
    WHEN bicicleta='O' THEN 'Otra' END as bicicleta"])

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
(def cartas-sql
  "SELECT
  id,
  categoria,
  sexo,
  bicicleta,
  no_participacion,
  nombre,
  apellido_materno,
  apellido_paterno,
  equipo,
  direccion,
  pais,
  ciudad,
  telefono,
  celular,
  email,
  tutor,
  DATE_FORMAT(dob,'%m/%d/%Y') as dob
  FROM cartas
  WHERE email = ?")

(def form-sql
  "SELECT
  id,
  categoria,
  sexo,
  bicicleta,
  no_participacion,
  nombre,
  apellido_materno,
  apellido_paterno,
  equipo,
  direccion,
  pais,
  ciudad,
  telefono,
  celular,
  email,
  tutor,
  DATE_FORMAT(dob,'%m/%d/%Y') as dob
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

(defn get-sexo-desc [s]
  (cond
    (= s "M") "Masculino"
    (= s "F") "Femenil"))

(defn email-body [row]
  (let [no_participacion (:no_participacion row)
        nombre (str (:nombre row) " " (:apellido_paterno row) " " (:apellido_materno row))
        email (:email row)
        categoria (get-categorias-desc (:categoria row))
        equipo (:equipo row)
        sexo (get-sexo-desc (:sexo row))
        direccion (:direccion row)
        telefono (:telefono row)
        edad (age (:dob row))
        celular (:celular row)]
    (str "
<html>
  <body>
    <h1>Serial Ciclista de Mexicali 2019</h1>
    <h4># Inscripción: " no_participacion "</h4>
    <u>Detalles de la inscripción</u>
    <div>
      <p><b>Nombre del Evento:</b> Serial Ciclista de Mexicali 2019</p>
      <p><b>Fecha del Evento:</b> 20/01/2019</p>
      <p><b>En la Ciudad:</b> Mexicali B.C.</p>
      <p><b>Contacto:</b> Marcopescador@hotmail.com</p>
      <p>El Numero se Entregara una hora antes del dia del evento en la parte frontal de las instalaciones del IMDECUF el Domingo 20 de Enero 2019. Para poder Solicitar el numero ya pagado, solo lo podrá hacer el atleta titular y el representante de equipo, presentando una identificación oficial.</p>
      <p><h4>Nombre del Participante: " nombre "</h4></p>
      <p><b>Correo del Participante:</b> " email " </p>
      <p><b>Categoria en que competirá:</b> " categoria ".</p>
      <p><b>Equipo:</b> " equipo "</p>
      <p><b>Edad:</b> " edad " <b>Sexo:</b> " sexo "</p>
      <p><b>Direccion:</b> " direccion "</p>
      <p><b>Telefono:</b> " telefono "</p>
      <p><b>Celular:</b> " celular "</p>
      <p><b>Nombre del padre o tutor (En su caso):</b></p>
      <br/>
      <p><b><i>Cómo realizar el pago</i></b></p>
      <p><b>Instrucciones para el cajero</b></p>
      <p>
      <ul>
        <li>Realizar el Pago en cualquier banco <b>HSBC</b> o directamente en cualquier tienda <b>OXXO</b></li>
        <li>Número de cuenta <b>HSBC: 4910 8960 8405 3810</b></li>
        <li>Realiza el pago de $100.00 pesos (Cien pesos) por el concepto de Inscripción de Serial Ciclista de Mexicali 2019</li>
        <li>Conserva el ticket para cualquier aclaración y enviarlo a la dirección de correo Marcopescador@hotmail.com o al inbox https://www.facebook.com/pescador.marco</li>
      </ul>
      </p>
    </div>
  </body>
</html>
       ")))

(defn exoneracion-save
  [{params :params}]
  (try
    (let [id       (fix-id (:id params))
          email (:email params)
          row (Query db [cartas-sql email])
          no_participacion (or (:no_participacion (first row)) (zpl (get-counter) 4))
          numero   (str (parse-int (:no_participacion params)))
          postvars {:id               id
                    :categoria        (:categoria params)
                    :sexo             (:sexo params)
                    :dob              (format-date-internal (:dob params))
                    :bicicleta        (:bicicleta params)
                    :no_participacion no_participacion
                    :nombre           (capitalize-words (:nombre params))
                    :apellido_paterno (capitalize-words (:apellido_paterno params))
                    :apellido_materno (capitalize-words (:apellido_materno params))
                    :equipo           (capitalize-words (:equipo params))
                    :direccion        (capitalize-words (:direccion params))
                    :pais             (capitalize-words (:pais params))
                    :ciudad           (capitalize-words (:ciudad params))
                    :telefono         (:telefono params)
                    :celular          (:celular params)
                    :email            (:email params)
                    :tutor            (capitalize-words (:tutor params))}
          result   (Save db :cartas postvars ["id = ?" id])
          body     {:from "marcopescador@hotmail.com"
                    :to (:email params)
                    :cc "marcopescador@hotmail.com"
                    :subject "Serial Ciclista de Mexicali 2019"
                    :body [{:type "text/html;charset=utf-8"
                            :content (email-body postvars)}]}]
      (if (seq result)
        (do
          (if (nil? id) (send-email host body))
          (generate-string {:success "Correctamente Processado!"}))
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
  CASE WHEN p.sexo = 'M' THEN 'Varonil'
  WHEN p.sexo = 'F' THEN 'Femenil' END as sexo,
  CASE WHEN p.bicicleta = 'R' THEN 'Bicicleta de ruta'
  WHEN p.bicicleta = 'M' THEN 'Bicicleta de montaña'
  WHEN p.bicicleta = 'F' THEN 'Bicicleta fija/SS' 
  WHEN p.bicicleta = 'O' THEN 'Otra' END as bicicleta,
  p.no_participacion,
  p.nombre,
  p.apellido_paterno,
  p.apellido_materno,
  p.equipo,
  p.direccion,
  p.pais,
  p.ciudad,
  p.telefono,
  p.celular,
  p.email,
  p.tutor
  FROM cartas p
  JOIN categorias s on s.id = p.categoria
  WHERE p.id = ?")

(defn build-name [row]
  (let [p1 (:nombre row)
        p2 (:apellido_paterno row nil)
        p3 (:apellido_materno row nil)
        nombre-completo (str p1 " " (if-not (nil? p2) p2) " " (if-not (nil? p3) p3))]
    nombre-completo))

(def build-body-p1
  "El que suscribe, por mi propio derecho, expresamente manifiesto que es mi deseo participar en el evento denominado
  \"Serial Ciclista de Mexicali 2019\", que se realizara en la avenida Reforma y calle K en la ciudad de Mexicali B.C. el
  domingo 2 de Diciembre del año 2018 de 9:00am a 3:00 pm. Así mismo me comprometo y obligo a no ingresar cualquier
  Área RESTRINGIDA(S) (entendida como aquella que requiera la autorización expresa mediante la expedición de
  credencial o permiso por parte del comité organizador, en razón de lo anterior al firmar el presente escrito acepto todos y
  cada uno de los términos y condiciones estipulados en el presente escrito:")

(defn build-body-p2 [row]
  (str "Yo " (build-name row) ", Con el número de participación " (:no_participacion row) " por el solo hecho de firmar
este documento, accepto cualquier y todos los riesgos y peligros que sobre mi persona recaigan en cuanto a mi participación
en Evento ciclista denominado \"Serial Ciclista de Mexicali 2019\". Por lo tanto, yo soy el único responsable de mi salud.
cualquier consecuencia, accidentes, perjuicios, deficiencias que puedan causar, de cualquier manera, posibles alteraciones
a mi salud, integridad fisica, o inclusive muerte. Por esa razón libero de cualquier responsabilidad al respecto al Comité
Organizador de dicho Evento y/o a las Asociaciones que lo integran, asi como a sus directores, patrocinadores y
representantes, y por medio de este conducto renuncio, sin limitación alguna a cualquier derecho, demanda o
indemnización al respecto. Reconozco y acepto que Comité Organizador del Evento \"Serial Ciclista de Mexicali 2019\"
no son ni serán consideradas responsables por cualquier desperfecto, pérdida o robo relacionados con mis pertenencias
personales."))

(def build-body-p3
  "También reconozco y acepto que como participante del EVENTO \"Serial Ciclista de Mexicali 2019\", deberé portar en
  todo momento el número de participante proporcionado por los organizadores del EVENTO, en el entendido que dicho
  número no podrán ser transferidas o intercambiadas con cualquier tercero bajo ningún concepto, por lo que si no cuento
  con la misma, los organizadores del EVENTO, podrán retirarme del mismo, leberándolos de toda responsabilidad, asi como
  renunciando a ejercer cualquier acción legal en su contra por las acciones tomadas a este respecto.")

(def build-body-p4
  "Así mismo, autorizo al comité organizador del evento \"Serial Ciclista de Mexicali 2019\", y/o a quien ésta designe, el uso
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
   [[:cell {:style :bold :colspan 3} (str "Firma del participante y/o tutor: ")]]])

(defn execute-report [id]
  (let [h1  "CIRCUITO CICLISTA NAVIDEÑO"
        h2  "OBREGON"
        h3  "CARTA DE EXONERACION"
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
         [:paragraph {:align :justified :indent-left 28 :indent-right 28} build-body-p1]
         [:spacer]
         [:paragraph {:align :justified :indent-left 28 :indent-right 28} (build-body-p2 row)]
         [:spacer]
         [:paragraph {:align :justified :indent-left 28 :indent-right 28} build-body-p3]
         [:spacer]
         [:paragraph {:align :justified :indent-left 28 :indent-right 28} build-body-p4]
         [:spacer]
         (build-body row)]
        output-stream)))))

(defn exoneracion-pdf [id]
  (let [file-name (str "exoneracion_" id ".pdf")]
    {:headers {"Content-type"        "application/pdf"
               "Content-disposition" (str "attachment;filename=" file-name)}
     :body    (execute-report id)}))
;; End pdf


(defn cartas-processar [{params :params}]
  (let [email (:email params)
        row (Query db [cartas-sql email])
        result (if (seq row) 1 0)
        no_participacion nil]
    (render-file "cartas/exoneracion/exoneracion.html" {:title "Registro Serial Ciclista Mexicali"
                                                        :user (or (get-session-id) "Anonimo")
                                                        :no_participacion no_participacion
                                                        :row (generate-string (first row))
                                                        :exists result})))

(defroutes exoneracion-routes
  (GET "/registro" [] (cartas))
  (POST "/cartas/processar" request [] (cartas-processar request))
  (GET "/cartas/exoneracion" [] (exoneracion))
  (POST "/cartas/exoneracion/json/grid" request (grid-json request))
  (GET "/cartas/exoneracion/json/form/:id" [id] (form-json id))
  (POST "/cartas/exoneracion/save" request [] (exoneracion-save request))
  (POST "/cartas/exoneracion/delete" request [] (exoneracion-delete request))
  (GET "/cartas/exoneracion/pdf/:id" [id] (exoneracion-pdf id)))
