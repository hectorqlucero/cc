(ns cc.routes.admin
  (:require [cc.routes.admin.carreras :refer [carreras-routes]]
            [cc.routes.admin.cuadrantes :refer [cuadrantes-routes]]
            [cc.routes.admin.taller :refer [taller-routes]]
            [cc.routes.admin.users :refer [users-routes]]
            [compojure.core :refer [defroutes]]))

(defroutes admin-routes
  users-routes
  cuadrantes-routes
  carreras-routes
  taller-routes)
