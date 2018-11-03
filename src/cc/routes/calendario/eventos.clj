(ns cc.routes.calendario.eventos
  (:require [cc.models.crud :refer :all]
            [cc.models.email :refer [host send-email]]
            [cc.models.grid :refer :all]
            [cc.models.util :refer :all]
            [cheshire.core :refer :all]
            [compojure.core :refer :all]
            [selmer.parser :refer [render-file]]))

(defn eventos
  []
  (render-file "calendario/eventos/index.html" {:title "Calendario - Eventos"
                                                   :user  (or (get-session-id) "Anonimo")}))

;;start eventos grid
(def search-columns
  ["id"
   "descripcion_corta"
   "descripcion"
   "punto_reunion"
   "CASE WHEN repetir = 'T' THEN 'Si' ELSE 'No' END"
   "DATE_FORMAT(fecha,'%m/%d/%Y')"
   "TIME_FORMAT(hora,'%h:%i %p')"
   "leader"])

(def aliases-columns
  ["id"
   "descripcion_corta"
   "descripcion"
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
                     (= user "Anonimo") (grid-search-extra search "anonimo = 'T' and rodada = 'F'")
                     (= level "U")      (grid-search-extra search (str "leader_email = '" email "'" " and rodada='F'"))
                     (= level "A")      (grid-search-extra search (str "leader_email = '" email "'" " and rodada='F'"))
                     :else (grid-search-extra search "rodada = 'F'"))
          order    (grid-sort (:sort params nil) (:order params nil))
          offset   (grid-offset (parse-int (:rows params)) (parse-int (:page params)))
          rows     (grid-rows table aliases join search order offset)]
      (generate-string rows))
    (catch Exception e (.getMessage e))))
;;end eventos grid

;;start eventos form
(def form-sql
  "SELECT id as id,
  descripcion,
  descripcion_corta,
  punto_reunion,
  DATE_FORMAT(fecha,'%m/%d/%Y') as fecha,
  TIME_FORMAT(hora,'%H:%i') as hora,
  leader,
  leader_email,
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
;;end eventos form

(defn eventos-save
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
                    :fecha             (format-date-internal (:fecha params))
                    :hora              (fix-hour (:hora params))
                    :leader            (:leader params)
                    :leader_email      (:leader_email params)
                    :cuadrante         (:cuadrante params)
                    :repetir           repetir
                    :rodada            "F"
                    :anonimo           anonimo}
          result   (Save db :rodadas postvars ["id = ?" id])]
      (if (seq result)
        (generate-string {:success "Correctamente Processado!"})
        (generate-string {:error "No se pudo processar!"})))
    (catch Exception e (.getMessage e))))

;;Start eventos-delete
(defn build-recipients [eventos_id]
  (into [] (map #(first (vals %)) (Query db ["SELECT email from rodadas_link where rodadas_id = ?" eventos_id]))))

(defn email-delete-body [eventos_id]
  (let [row               (first (Query db ["SELECT leader,leader_email,descripcion_corta FROM rodadas where id = ?" eventos_id]))
        leader            (:leader row)
        leader_email      (:leader_email row)
        descripcion_corta (:descripcion_corta row)
        content           (str "<strong>Hola:</strong></br></br>La rodada organizada por: " leader " <a href='mailto:"leader_email"'>" leader_email "</a> se cancelo.  Disculpen la inconveniencia que esto pueda causar.</br>"
                               "<small><strong>Nota:</strong><i> Si desea contestarle a esta persona, por favor hacer clic en el email arriba!</i></br></br>"
                               "Muchas gracias por su participacion y esperamos que la proxima vez se pueda realizar la rodada.</br></br>"
                               "<small>Esta es un aplicación para todos los ciclistas de Mexicali. se aceptan sugerencias.  <a href='mailto: hectorqlucero@gmail.com'>Clic aquí para mandar sugerencias</a></small>")
        recipients        (build-recipients eventos_id)
        body              {:from    "hectorqlucero@gmail.com"
                           :to      recipients
                           :cc      "hectorqlucero@gmail.com"
                           :subject (str descripcion_corta " - Cancelacion")
                           :body    [{:type    "text/html;charset=utf-8"
                                      :content content}]}]
    body))

(defn eventos-delete
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
;;End eventos-delete

(defroutes eventos-routes
  (GET "/calendario/eventos" [] (eventos))
  (POST "/calendario/eventos/json/grid" request (grid-json request))
  (GET "/calendario/eventos/json/form/:id" [id] (form-json id))
  (POST "/calendario/eventos/save" request [] (eventos-save request))
  (POST "/calendario/eventos/delete" request [] (eventos-delete request)))

