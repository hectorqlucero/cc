(ns cc.routes.appointments
  (:require [cc.models.crud :refer :all]
            [cc.models.grid :refer :all]
            [cc.models.util :refer :all]
            [cheshire.core :refer :all]
            [compojure.core :refer :all]
            [selmer.parser :refer [render-file]]))

(defn appointments [request]
  (render-file "appointments/index.html" {:title "Appointments"}))

;;start appointments grid
(def search-columns
  ["appointments.id"
   "CONCAT(student.first_name,' ',student.last_name)"
   "appointments.user_id"
   "CONCAT(users.firstname,' ',users.lastname)"
   "appointments.title"
   "DATE_FORMAT(appointments.a_date,'%m/%d/%Y')"
   "appointments.start_time"
   "appointments.end_time"
   "CASE WHEN appointments.allday = 'T' THEN 'Yes' ELSE 'No' END"
   "CASE WHEN appointments.status = 'T' THEN 'Pending' WHEN appointments.status = 'X' THEN 'Remove' WHEN appointments.status = 'O' THEN 'Completed on time' WHEN appointments.status = 'L' THEN 'Completed Late' WHEN appointments.status = 'E' THEN 'Completed before time' WHEN appointments.status = 'S' THEN 'Reprogramed by user' WHEN appointments.status = 'Z' THEN 'Cancelled by user' ELSE 'Unknown' END"])

(def aliases-columns
  ["appointments.id as id"
   "CONCAT(student.first_name,' ',student.last_name) as student_id"
   "appointments.user_id as user_id"
   "CONCAT(users.firstname,' ',users.lastname) as username"
   "appointments.title"
   "DATE_FORMAT(appointments.a_date,'%m/%d/%Y') as a_date"
   "appointments.start_time"
   "appointments.end_time"
   "CASE WHEN appointments.allday = 'T' THEN 'Yes' ELSE 'No' END AS allday"
   "CASE WHEN appointments.status = 'T' THEN 'Pending' WHEN appointments.status = 'X' THEN 'Remove' WHEN appointments.status = 'O' THEN 'Completed on time' WHEN appointments.status = 'L' THEN 'Completed Late' WHEN appointments.status = 'E' THEN 'Completed before time' WHEN appointments.status = 'S' THEN 'Reprogramed by user' WHEN appointments.status = 'Z' THEN 'Cancelled by user' ELSE 'Unknown' END AS status"])

(defn grid-json [request]
  (try
    (let [table    "appointments"
          scolumns (convert-search-columns search-columns)
          aliases  aliases-columns
          join     "JOIN users on users.id = appointments.user_id
                JOIN student on student.id = appointments.student_id"
          search   (grid-search (:search (:params request) nil) scolumns)
          order    (grid-sort (:sort (:params request) nil) (:order (:params request) nil))
          offset   (grid-offset (parse-int (:rows (:params request))) (parse-int (:page (:params request))))
          rows     (grid-rows table aliases join search order offset)]
      (generate-string (grid-rows table aliases join search order offset)))
    (catch Exception e (.getmessage e))))
;;end appointments grid

;;start appointments form
(def form-sql
  "SELECT id as id,
  student_id as student_id,
  user_id,
  title,
  DATE_FORMAT(a_date, '%m/%d/%Y') as a_date,
  allday,
  start_time,
  end_time,
  status
  FROM appointments
  WHERE id = ?")

(defn form-json [id]
  (let [record (Query db [form-sql id])]
    (generate-string (first record))))
;;end appointment form

(defn appointments-save [request]
  (let [row      (:params request)
        id       (:id row "0")
        postvars {:id         id
                  :student_id (:student_id row)
                  :user_id    (:user_id row)
                  :title      (:title row)
                  :a_date     (format-date-internal (:a_date row))
                  :start_time (:start_time row)
                  :end_time   (:end_time row)
                  :allday     (:allday row "F")
                  :status     (:status row "T")}]
    (if (Save db :appointments postvars ["id = ?" id])
      (generate-string {:success "Processed Successfully!"})
      (generate-string {:error "Unable to process request!"}))))

(defn appointments-delete [request]
  (let [id (:id (:params request))]
    (Delete db :appointments ["id = ?" id])
    (generate-string {:success "Removed successfully!"})))

(defroutes appointments-routes
  (GET "/appointments" request [] (appointments request))
  (POST "/appointments/json/grid" request [] (grid-json request))
  (GET "/appointments/json/form/:id" [id] (form-json id))
  (POST "/appointments/save" request [] (appointments-save request))
  (POST "/appointments/delete" request [] (appointments-delete request)))
