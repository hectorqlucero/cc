(ns cc.routes.entrenamiento.rodadas
  (:require [cc.models.crud :refer :all]
            [cc.models.grid :refer :all]
            [cc.models.util :refer :all]
            [cheshire.core :refer :all]
            [compojure.core :refer :all]
            [selmer.parser :refer [render-file]]))

(defn rodadas
  []
  (render-file "entrenamiento/rodadas/index.html" {:title "Entrenamiento - Rodadas"}))

;;start rodadas grid
(def search-columns
  ["id"
   "descripcion_corta"
   "descripcion"
   "punto_reunion"
   "DATE_FORMAT(fecha,'%m/%d/%Y')"
   "TIME_FORMAT(hora,'%h:%i %p')"
   "leader"])

(def aliases-columns
  ["id"
   "descripcion_corta"
   "descripcion"
   "punto_reunion"
   "DATE_FORMAT(fecha,'%m/%d/%Y') as fecha"
   "TIME_FORMAT(hora,'%h:%i %p') as hora"
   "leader"])

(defn grid-json
  [{params :params}]
  (try
    (let [table    "rodadas"
          scolumns (convert-search-columns search-columns)
          aliases  aliases-columns
          join     ""
          search   (grid-search (:search params nil) scolumns)
          order    (grid-sort (:sort params nil) (:order params nil))
          offset   (grid-offset (parse-int (:rows params)) (parse-int (:page params)))
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
  DATE_FORMAT(fecha,'%m/%d/%Y') as fecha,
  TIME_FORMAT(hora,'%H:%i') as hora,
  leader,
  leader_email,
  repetir
  FROM rodadas
  WHERE id = ?")

(defn form-json
  [id]
  (try
    (let [row (Query db [form-sql id])]
      (generate-string (first row)))
    (catch Exception e (.getMessage e))))
;;end rodadas form

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
    (let [rodadas_id (fix-id (:rodadas_id params))
          email      (:email params)
          postvars   {:rodadas_id  rodadas_id
                      :user        (:user params)
                      :comentarios (:comentarios params)
                      :email       email}
          result     (Save db :rodadas_link postvars ["rodadas_id = ? and email = ?" rodadas_id email])
          ]
      (if (seq result)
        (generate-string {:success "Correctamente Processado!"})
        (generate-string {:error "No se pudo processar!"})))
    (catch Exception e (.getMessage e))))

(defn rodadas-save
  [{params :params}]
  (try
    (let [id       (fix-id (:id params))
          postvars {:id                id
                    :descripcion       (:descripcion params)
                    :descripcion_corta (:descripcion_corta params)
                    :punto_reunion     (:punto_reunion params)
                    :fecha             (format-date-internal (:fecha params))
                    :hora              (fix-hour (:hora params))
                    :leader            (:leader params)
                    :leader_email      (:leader_email params)
                    :repetir           (:repetir params)}
          result   (Save db :rodadas postvars ["id = ?" id])]
      (if (seq result)
        (generate-string {:success "Correctamente Processado!"})
        (generate-string {:error "No se pudo processar!"})))
    (catch Exception e (.getMessage e))))

(defn rodadas-delete
  [{params :params}]
  (try
    (let [id     (:id params nil)
          result (if-not (nil? id)
                   (Delete db :rodadas ["id = ?" id])
                   nil)]
      (if (seq result)
        (generate-string {:success "Removido appropiadamente!"})
        (generate-string {:error "No se pudo remover!"})))
    (catch Exception e (.getMessage e))))

(defroutes rodadas-routes
  (GET "/entrenamiento/rodadas" [] (rodadas))
  (POST "/entrenamiento/rodadas/json/grid" request (grid-json request))
  (GET "/entrenamiento/rodadas/json/form/:id" [id] (form-json id))
  (GET "/entrenamiento/rodadas/asistir/:id" [id] (form-asistir id))
  (POST "/entrenamiento/rodadas/asistir" request [] (form-asistir-save request))
  (POST "/entrenamiento/rodadas/save" request [] (rodadas-save request))
  (POST "/entrenamiento/rodadas/delete" request [] (rodadas-delete request)))
