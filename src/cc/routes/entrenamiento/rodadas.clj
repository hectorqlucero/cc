(ns cc.routes.entrenamiento.rodadas
  (:require [cc.models.crud :refer :all]
            [cc.models.email :refer [host send-email]]
            [cc.models.grid :refer :all]
            [cc.models.util :refer :all]
            [cheshire.core :refer :all]
            [compojure.core :refer :all]
            [selmer.parser :refer [render-file]]))

(defn rodadas
  []
  (render-file "entrenamiento/rodadas/index.html" {:title "Entrenamiento - Rodadas"
                                                   :user  (or (get-session-id) "Anonimo")}))

;;start rodadas grid
(def search-columns
  ["id"
   "descripcion_corta"
   "descripcion"
   "punto_reunion"
   "CASE WHEN nivel = 'P' THEN 'Principiantes' WHEN nivel = 'M' THEN 'Medio' WHEN nivel = 'A' THEN 'Avanzado' WHEN nivel = 'T' THEN 'TODOS' END"
   "distancia"
   "velocidad"
   "CASE WHEN repetir = 'T' THEN 'Si' ELSE 'No' END"
   "DATE_FORMAT(fecha,'%m/%d/%Y')"
   "TIME_FORMAT(hora,'%h:%i %p')"
   "leader"])

(def aliases-columns
  ["id"
   "descripcion_corta"
   "descripcion"
   "CASE WHEN nivel = 'P' THEN 'Principiantes' WHEN nivel = 'M' THEN 'Medio' WHEN nivel = 'A' THEN 'Avanzado' WHEN nivel = 'T' THEN 'TODOS' END AS nivel"
   "distancia"
   "velocidad"
   "punto_reunion"
   "CASE WHEN repetir = 'T' THEN 'Si' ELSE 'No' END as repetir"
   "DATE_FORMAT(fecha,'%m/%d/%Y') as fecha"
   "TIME_FORMAT(hora,'%h:%i %p') as hora"
   "leader"])

(defn grid-json
  [{params :params}]
  (try
    (let [table    "rodadas"
          user     (or (get-session-id) "Anonimo")
          level    (user-level)
          email    (user-email)
          scolumns (convert-search-columns search-columns)
          aliases  aliases-columns
          join     ""
          search   (grid-search (:search params nil) scolumns)
          search   (cond
                     (= user "Anonimo") (grid-search-extra search "anonimo = 'T'")
                     (= level "U")      (grid-search-extra search (str "leader_email = '" email "'"))
                     (= level "A")      (grid-search-extra search (str "leader_email = '" email "'"))
                     :else (grid-search (:search params nil) scolumns))
          order    (grid-sort (:sort params nil) (:order params nil))
          offset   (grid-offset (parse-int (:rows params)) (parse-int (:page params)))
          sql      (grid-sql table aliases join search order offset)
          rows     (grid-rows table aliases join search order offset)]
      (generate-string rows))
    (catch Exception e (.getMessage e))))
;;end rodadas grid

;;start rodadas form
(def form-sql
  "SELECT id as id,
  descripcion,
  descripcion_corta,
  punto_reunion,
  nivel,
  distancia,
  velocidad,
  DATE_FORMAT(fecha,'%m/%d/%Y') as fecha,
  TIME_FORMAT(hora,'%H:%i') as hora,
  leader,
  leader_email,
  cuadrante,
  repetir,
  anonimo
  FROM rodadas
  WHERE id = ?")

(defn form-json
  [id]
  (try
    (let [row (Query db [form-sql id])]
      (generate-string (first row)))
    (catch Exception e (.getMessage e))))
;;end rodadas form

;;Start form-assistir
(defn email-body[rodadas_id user email comentarios asistir_desc]
  (let [row               (first (Query db ["SELECT leader,leader_email,descripcion_corta FROM rodadas WHERE id = ?" rodadas_id]))
        leader            (:leader row)
        leader_email      (:leader_email row)
        descripcion_corta (:descripcion_corta row)
        content           (str "<strong>Hola " leader ":</strong></br></br>"
                               "Mi nombre es <strong>" user "</strong> y mi correo electronico es <a href='mailto:" email"'>"email"</a> y estoy confirmando que <strong>"  asistir_desc "</strong> a la rodada.</br>"
                               "<small><strong>Nota:</strong><i> Si desea contestarle a esta persona, por favor hacer clic en el email arriba!</i></br></br>"
                               "<strong>Comentarios:</strong> " comentarios "</br></br>"
                               "<small>Esta es un aplicación para todos los ciclistas de Mexicali. se aceptan sugerencias.  <a href='mailto: hectorqlucero@gmail.com'>Clic aquí para mandar sugerencias</a></small>")
        body              {:from    "hectorqlucero@gmail.com"
                           :to      leader_email
                           :cc      "hectorqlucero@gmail.com"
                           :subject (str descripcion_corta " - Confirmar asistencia")
                           :body    [{:type    "text/html;charset=utf-8"
                                      :content content}]}]
    body))

(defn form-asistir
  [rodadas_id]
  (let [row        (first (Query db ["select descripcion_corta,DATE_FORMAT(fecha,'%m/%d/%Y') as fecha,TIME_FORMAT(hora,'%h:%i %p') as hora from rodadas where id = ?" rodadas_id]))
        event_desc (:descripcion_corta row)
        fecha      (:fecha row)
        hora       (:hora row)
        title      (str fecha " - " hora " [" event_desc "] Confirmar asistencia")]
    (render-file "entrenamiento/rodadas/asistir.html" {:title      title
                                                       :rodadas_id rodadas_id})))

(defn form-asistir-save
  [{params :params}]
  (try
    (let [rodadas_id   (fix-id (:rodadas_id params))
          email        (:email params)
          asistir_desc (if (= (:asistir params) "T")
                         "ASISTIRE"
                         "NO ASISTIRE")
          postvars     {:rodadas_id  rodadas_id
                        :user        (:user params)
                        :comentarios (:comentarios params)
                        :email       email
                        :asistir     (:asistir params)}
          body         (email-body rodadas_id (:user params) email (:comentarios params) asistir_desc)
          result       (Save db :rodadas_link postvars ["rodadas_id = ? and email = ?" rodadas_id email])]
      (if (seq result)
        (do
          (send-email host body)
          (generate-string {:success "Correctamente Processado!"}))
        (generate-string {:error "No se pudo processar!"})))
    (catch Exception e (.getMessage e))))
;;End form-assistir

(defn rodadas-save
  [{params :params}]
  (try
    (let [id       (fix-id (:id params))
          user     (or (get-session-id) "Anonimo")
          repetir  (if (= user "Anonimo") "F" (:repetir params))
          anonimo  (if (= user "Anonimo") "T" "F")
          postvars {:id                id
                    :descripcion       (:descripcion params)
                    :descripcion_corta (:descripcion_corta params)
                    :punto_reunion     (:punto_reunion params)
                    :nivel             (:nivel params)
                    :distancia         (:distancia params)
                    :velocidad         (:velocidad params)
                    :fecha             (format-date-internal (:fecha params))
                    :hora              (fix-hour (:hora params))
                    :leader            (:leader params)
                    :leader_email      (:leader_email params)
                    :cuadrante         (:cuadrante params)
                    :repetir           repetir
                    :anonimo           anonimo}
          result   (Save db :rodadas postvars ["id = ?" id])]
      (if (seq result)
        (generate-string {:success "Correctamente Processado!"})
        (generate-string {:error "No se pudo processar!"})))
    (catch Exception e (.getMessage e))))

;;Start rodadas-delete
(defn build-recipients [rodadas_id]
  (into [] (map #(first (vals %)) (Query db ["SELECT email from rodadas_link where rodadas_id = ?" rodadas_id]))))

(defn email-delete-body [rodadas_id]
  (let [row               (first (Query db ["SELECT leader,leader_email,descripcion_corta FROM rodadas where id = ?" rodadas_id]))
        leader            (:leader row)
        leader_email      (:leader_email row)
        descripcion_corta (:descripcion_corta row)
        content           (str "<strong>Hola:</strong></br></br>La rodada organizada por: " leader " <a href='mailto:"leader_email"'>" leader_email "</a> se cancelo.  Disculpen la inconveniencia que esto pueda causar.</br>"
                               "<small><strong>Nota:</strong><i> Si desea contestarle a esta persona, por favor hacer clic en el email arriba!</i></br></br>"
                               "Muchas gracias por su participacion y esperamos que la proxima vez se pueda realizar la rodada.</br></br>"
                               "<small>Esta es un aplicación para todos los ciclistas de Mexicali. se aceptan sugerencias.  <a href='mailto: hectorqlucero@gmail.com'>Clic aquí para mandar sugerencias</a></small>")
        recipients        (build-recipients rodadas_id)
        body              {:from    "hectorqlucero@gmail.com"
                           :to      recipients
                           :cc      "hectorqlucero@gmail.com"
                           :subject (str descripcion_corta " - Cancelacion")
                           :body    [{:type    "text/html;charset=utf-8"
                                      :content content}]}]
    body))

(defn rodadas-delete
  [{params :params}]
  (try
    (let [id     (:id params nil)
          result (if-not (nil? id)
                   (do
                     (send-email host (email-delete-body id))
                     (Delete db :rodadas ["id = ?" id]))
                   nil)]
      (if (seq result)
        (generate-string {:success "Removido appropiadamente!"})
        (generate-string {:error "No se pudo remover!"})))
    (catch Exception e (.getMessage e))))
;;End rodadas-delete

(defroutes rodadas-routes
  (GET "/entrenamiento/rodadas" [] (rodadas))
  (POST "/entrenamiento/rodadas/json/grid" request (grid-json request))
  (GET "/entrenamiento/rodadas/json/form/:id" [id] (form-json id))
  (GET "/entrenamiento/rodadas/asistir/:id" [id] (form-asistir id))
  (POST "/entrenamiento/rodadas/asistir" request [] (form-asistir-save request))
  (POST "/entrenamiento/rodadas/save" request [] (rodadas-save request))
  (POST "/entrenamiento/rodadas/delete" request [] (rodadas-delete request)))
