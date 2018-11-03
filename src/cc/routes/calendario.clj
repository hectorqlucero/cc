(ns cc.routes.calendario
  (:require [cc.routes.calendario.eventos :refer [eventos-routes]]
            [compojure.core :refer :all]))

(defroutes calendario-routes
  eventos-routes)
