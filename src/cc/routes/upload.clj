(ns cc.routes.upload
  (:require [clojure.java.io :as io]
            [compojure.core :refer :all]
            [ring.util.response :refer [redirect]]
            [selmer.parser :refer [render-file]]))

(def UPLOADS "/var/www/html/lucero-systems.cf/uploads/sf/")

(defn main [_]
  (render-file "uploads.html" {:title "Subir Fotos"}))

(defn upload-file [file rfile]
  (let [tempfile  (file :tempfile)
        real-name (file :filename)
        size      (:size file)
        type      (:content-type file)
        extension (peek (clojure.string/split type #"\/"))
        extension (if (= extension "jpeg") "jpg" "jpg")
        filename  (str rfile "." extension)
        result    (if-not (zero? size)
                    (do (io/copy tempfile (io/file (str UPLOADS filename)))))]
    result))

(defn upload-picture [{params :params}]
  (doseq [n (range 1 11)]
    (let [rfile (str "r" n)
          kfile (keyword rfile)
          file  (kfile params)]
      (upload-file file rfile)))
  (redirect "/"))

(defroutes upload-routes
  (GET "/file" req [] (main req))
  (POST "/file" req [] (upload-picture req)))
