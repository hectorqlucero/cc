(ns cc.routes.contrareloj
  (:require [cc.routes.contrareloj.crear_carrera :refer [crear_carreras-routes]]
            [compojure.core :refer :all]))

(defroutes contrareloj-routes
  crear_carreras-routes)
