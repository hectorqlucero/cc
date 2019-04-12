(ns cc.routes.cartas
  (:require [cc.routes.cartas.categorias :refer [categorias-routes]]
            [cc.routes.cartas.exoneracion :refer [exoneracion-routes]]
            [cc.routes.cartas.puntos :refer [puntos-routes]]
            [compojure.core :refer [defroutes]]))

(defroutes cartas-routes
  exoneracion-routes
  puntos-routes
  categorias-routes)
