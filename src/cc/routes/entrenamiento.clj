(ns cc.routes.entrenamiento
  (:require [cc.routes.entrenamiento.rodadas :refer [rodadas-routes]]
            [compojure.core :refer :all]))

(defroutes entrenamiento-routes
  rodadas-routes)
