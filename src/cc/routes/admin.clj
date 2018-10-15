(ns cc.routes.admin
  (:require [cc.routes.admin.cuadrantes :refer [cuadrantes-routes]]
            [cc.routes.admin.users :refer [users-routes]]
            [compojure.core :refer :all]))

(defroutes admin-routes
  users-routes
  cuadrantes-routes)
