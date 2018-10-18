(ns cc.models.email
  (:require [postal.core :refer [send-message]]))
;; Documentation for email
(comment
  (send-message {:host "mail.isp.net"}
                {:from    "me@draines.com"
                 :to      "foo@example.com"
                 :subject "Hi!"
                 :body    [{:type    "text/html"
                            :content "<b>Test!</b>"}
                           ;;;; supports both dispositions:
                           {:type    :attachment
                            :content (java.io.File. "/tmp/foo.txt")}
                           {:type         :inline
                            :content      (java.io.File. "/tmp/a.pdf")
                            :content-type "application/pdf"}]})
  {:code 0, :error :SUCCESS, :message "message sent"})      ;Returned error messages
;; End documentation for email

(defn send-email [body]
  (send-message {:host "mail.gmx.com"
                 :user "hectorqlucero@gmx.com"
                 :pass "Patito6853."
                 :tls  true}
                body))

