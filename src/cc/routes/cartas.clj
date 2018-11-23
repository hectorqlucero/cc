(ns cc.routes.cartas
  (:require [cc.routes.cartas.exoneracion :refer [exoneracion-routes]]
            [compojure.core :refer :all]))

(defroutes cartas-routes
  exoneracion-routes)
