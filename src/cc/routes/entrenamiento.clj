(ns cc.routes.entrenamiento
  (:require [cc.routes.entrenamiento.rodadas :refer [rodadas-routes]]
            [compojure.core :refer [defroutes]]))

(defroutes entrenamiento-routes
  rodadas-routes)
