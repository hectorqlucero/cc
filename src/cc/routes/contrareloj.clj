(ns cc.routes.contrareloj
  (:require [cc.routes.contrareloj.crear_carrera :refer [crear_carreras-routes]]
            [compojure.core :refer [defroutes]]))

(defroutes contrareloj-routes
  crear_carreras-routes)
