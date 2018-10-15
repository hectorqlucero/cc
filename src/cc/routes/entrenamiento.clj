(ns cc.routes.entrenamiento
  (:require [compojure.core :refer :all]
            [cc.routes.entrenamiento.rodadas :refer [rodadas-routes]]
            [clojure.java.io :as io]))

(defroutes entrenamiento-routes
  rodadas-routes)
