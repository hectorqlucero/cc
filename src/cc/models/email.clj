(ns cc.models.email
  (:require [postal.core :refer [send-message]]))
;;(send-message {:host "smtp.gmail.com"
;;               :user "hectorqlucero@gmail.com"
;;               :pass "patito6853."
;;               :ssl  true}
;;              {:from    "me@draines.com"
;;               :to      "foo@example.com"
;;               :subject "Hi!"
;;               :body    [{:type    "text/html"
;;                          :content "<b>Test!</b>"}
;;                           ;;;; supports both dispositions:
;;                         {:type    :attachment
;;                          :content (java.io.File. "/tmp/foo.txt")}
;;                         {:type         :inline
;;                          :content      (java.io.File. "/tmp/a.pdf")
;;                          :content-type "application/pdf"}]})
;;{:code 0, :error :SUCCESS, :message "message sent"}      ;Returned error messages
;;
;;{:host "mail.gmx.com"
;; :user "hectorqlucero@gmx.com"
;; :pass "Patito6853."
;; :tls  true}
;;
;;{:host "smtp.gmail.com"
;; :user "hectorqlucero@gmx.com"
;; :pass "patito6853."
;; :ssl  true}

(def host
  {:host "mail.gmx.com"
   :user "hectorqlucero@gmx.com"
   :pass "Patito6853."
   :tls  true})

(def body
  {:from    "hectorqlucero@gmx.com"
   :to      "marthalucero56@gmail.com"
   :subject "Hi!"
   :body    [{:type    "text/html"
              :content "<b>Testing</b>"}]})

(defn send-email [host body]
  (send-message host body))

;;(send-email host body)
