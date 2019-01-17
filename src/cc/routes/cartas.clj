(ns cc.routes.cartas
  (:require [cc.routes.cartas.exoneracion :refer [exoneracion-routes]]
            [cc.routes.cartas.puntos :refer [puntos-routes]]
            [compojure.core :refer :all]))

(defroutes cartas-routes
  exoneracion-routes
  puntos-routes)
