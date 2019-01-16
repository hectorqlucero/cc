(ns cc.routes.admin
  (:require [cc.routes.admin.cuadrantes :refer [cuadrantes-routes]]
            [cc.routes.admin.users :refer [users-routes]]
            [cc.routes.admin.carreras :refer [carreras-routes]]
            [compojure.core :refer :all]))

(defroutes admin-routes
  users-routes
  cuadrantes-routes
  carreras-routes)
