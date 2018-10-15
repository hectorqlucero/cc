(ns cc.models.cdb
  (:require [cc.models.crud :refer :all]
            [cc.models.util :refer [today-internal]]
            [noir.util.crypt :as crypt]
            [clojure.java.jdbc :as j]))

(def users-sql
  "CREATE TABLE users (
  id int(11) NOT NULL AUTO_INCREMENT,
  lastname varchar(45) DEFAULT NULL,
  firstname varchar(45) DEFAULT NULL,
  username varchar(45) DEFAULT NULL,
  password TEXT DEFAULT NULL,
  dob varchar(45) DEFAULT NULL,
  cell varchar(45) DEFAULT NULL,
  phone varchar(45) DEFAULT NULL,fax varchar(45) DEFAULT NULL,
  email varchar(100) DEFAULT NULL,
  level char(1) DEFAULT NULL COMMENT 'A=Administrator,U=User',
  active char(1) DEFAULT NULL COMMENT 'T=Active,F=Not active',
  PRIMARY KEY (id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8")

(def appointments-sql
  "CREATE TABLE appointments (
  id int(11) NOT NULL AUTO_INCREMENT,
  student_id int(11) DEFAULT NULL,
  user_id int(11) DEFAULT NULL,
  title TEXT DEFAULT NULL,
  a_date date DEFAULT NULL,
  start_time time DEFAULT NULL,
  end_time time DEFAULT NULL,
  allday char(1) DEFAULT NULL COMMENT 'T=Yes,F=No',
  status char(1) DEFAULT NULL COMMENT 'T=Pending\nX=Remove\nO=Completed on time\nL=Completed late\nE=Completed before time\nS=Reprogrammed by user\nZ=Cancelled by user',
  PRIMARY KEY (id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8")

(def student-sql
  "CREATE TABLE student (
  id int(11) NOT NULL AUTO_INCREMENT,
  first_name varchar(45) DEFAULT NULL,
  last_name varchar(45) DEFAULT NULL,
  dob date DEFAULT NULL,
  gender char(1) DEFAULT NULL COMMENT 'M=Male,F=Female',
  enrollment_date date DEFAULT NULL,
  withdrawn_date date DEFAULT NULL,
  address varchar(200) DEFAULT NULL,
  city varchar(100) DEFAULT NULL,
  state varchar(50) DEFAULT 'Baja California',
  phone varchar(50) DEFAULT NULL,
  cell varchar(50) DEFAULT NULL,
  PRIMARY KEY (id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8")

(def cuadrantes-sql
  "CREATE TABLE cuadrantes (
  id int(11) NOT NULL AUTO_INCREMENT,
  name varchar(100) DEFAULT NULL,
  leader varchar(100) DEFAULT NULL,
  leader_phone varchar(45) DEFAULT NULL,
  leader_cell varchar(45) DEFAULT NULL,
  leader_email varchar(100) DEFAULT NULL,
  notes TEXT DEFAULT NULL,
  status char(1) DEFAULT NULL COMMENT 'T=Active,F=Inactive',
  PRIMARY KEY (id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8")

(def rodadas-sql
  "CREATE TABLE rodadas (
  id int(11) NOT NULL AUTO_INCREMENT,
  descripcion_corta varchar(100),
  descripcion varchar(3000) DEFAULT NULL,
  punto_reunion varchar(1000) DEFAULT NULL,
  fecha date DEFAULT NULL,
  hora time DEFAULT NULL,
  leader varchar(100) DEFAULT NULL,
  leader_email varchar(100) DEFAULT NULL,
  repetir char(1) DEFAULT NULL COMMENT 'T=Si,F=No',
  PRIMARY KEY (id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8")

(def rodadas_link-sql
  "CREATE TABLE rodadas_link (
  id int(11) NOT NULL AUTO_INCREMENT,
  rodadas_id int(11) NOT NULL,
  user varchar(200) DEFAULT NULL,
  comentarios TEXT DEFAULT NULL,
  email varchar(100) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY rodadas_id (rodadas_id),
  CONSTRAINT rodadas_link_ibfk_1 FOREIGN KEY (rodadas_id) REFERENCES rodadas (id) ON DELETE CASCADE ON UPDATE NO ACTION
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8")

(def cuadrantes-rows
  [{:name         "Rositas"
    :leader       "Rossy Rutiaga"
    :leader_email "rossyrutiaga@rositas.com"
    :notes        "Cuadrante ciclista con todos los niveles para el gusto del ciclista."
    :status       "T"}
   {:name         "Azules"
    :leader       "Ana Villa"
    :leader_email "anavilla@azules.com"
    :notes        "Cuadrante ciclista con niveles inter y fast."
    :status       "T"}])

(def rodadas-rows
  [{:descripcion_corta "San Lunes"
    :descripcion       "Ruta que puede variar por las calles de la ciudad. Se rodaran por lo menos 20 kilometros.  No olvidar traer casco, luces, auga y un tubo de repuesto."
    :punto_reunion     "Parue Hidalgo"
    :fecha             "2018-10-08"
    :hora              "20:00:00"
    :leader            "Christian"
    :leader_email      "christian@rositas.com"
    :repetir           "T"}
   {:descripcion_corta "Santa Isabel"
    :descripcion       "Salimos del Parque Hidalgo hacia la Santa Isabel.  Hidratacion en la OXXO que esta en la Lazaro Cardenas.  No olividen traer casco, luces, agua y un tubo de repuesto."
    :punto_reunion     "Parque Hidalgo"
    :fecha             "2018-10-09"
    :hora              "20:00:00"
    :leader            "Ruth"
    :leader_email      "ruth@rositas.com"
    :repetir           "T"}
   {:descripcion_corta "Canalera"
    :descripcion       "Salimos por el canal de la Independencia a veces hasta el aeropuerto.  No olviden traer casco, luces, agua y un tubo de repuesto."
    :punto_reunion     "Parque Hidalgo"
    :fecha             "2018-10-08"
    :hora              "20:00:00"
    :leader            "Humberto"
    :leader_email      "humberto@rositas.com"
    :repetir           "T"}
   {:descripcion_corta "Adorada"
    :descripcion       "Ruta que puede variar entre el Campestre y el Panteon rumbo al aeropuerto.  No olviden traer casco, luces, agua y un tubo de repuesto."
    :punto_reunion     "Parque Hidalgo"
    :fecha             "2018-10-10"
    :hora              "20:00:00"
    :leader            "Martha Parada"
    :leader_email      "adorada@rositas.com"
    :repetir           "T"}
   {:descripcion_corta "Culinaria"
    :descripcion       "Salimos del parque Hidalgo hacia el panteon que esta rumbo al aeropuerto. No olviden traer casco, luces, agua y un tubo de repuesto."
    :punto_reunion     "Parque Hidalgo"
    :fecha             "2018-10-11"
    :hora              "20:00:00"
    :leader            "Chefsito"
    :leader_email      "chefsito@rositas.com"
    :repetir           "T"}
   {:descripcion_corta "Intermedia"
    :descripcion       "Salimos del parque Hidalgo hacia el hotel que esta despues del OXXO.  No olviden traer casco, luces, agua y un tubo de repuesto."
    :punto_reunion     "Parque Hidalgo"
    :fecha             "2018-10-11"
    :hora              "20:00:00"
    :leader            "Humberto"
    :leader_email      "humberto@rositas.com"
    :repetir           "T"}
   {:descripcion_corta "Pedacera"
    :descripcion       "Salimos del parque Hidalgo hacia el aeropuerto.  No olviden traer casco, luces, agua y un tubo de repuesto."
    :punto_reunion     "Parque Hidalgo"
    :fecha             "2018-10-11"
    :hora              "20:00:00"
    :leader            "Oscar Raul"
    :leader_email      "oscarraul@rositas.com"
    :repetir           "T"}
   {:descripcion_corta "Familiar"
    :descripcion       "Salimos del Parque Hidalgo con ruta indefinida.  No olviden traer casco, luces, agua y un tubo de repuesto."
    :punto_reunion     "Parque Hidalgo"
    :fecha             "2018-10-12"
    :hora              "20:00:00"
    :leader            "Jose el Pechocho"
    :leader_email      "pechocho@rositas.com"
    :repetir           "T"}])

(def rodadas_link-rows
  [{:rodadas_id  "1"
    :user        "Hector Lucero"
    :comentarios "Alli estare en punto."
    :email       "hectorqlucero@gmail.com"}
   {:rodadas_id  "1"
    :user        "Martha Lucero"
    :comentarios "Alli estaremos"
    :email       "marthalucero56@gmail.com"}
   {:rodadas_id  "2"
    :user        "Martha Lucero"
    :comentarios "Alli estaremos"
    :email       "marthalucero56@gmail.com"}
   {:rodadas_id  "2"
    :user        "Hector Lucero"
    :comentarios "Alli estaremos"
    :email       "hectorqlucero@gmail.com"}
   {:rodadas_id  "3"
    :user        "Martha Lucero"
    :comentarios "Alli estaremos"
    :email       "marthalucero56@gmail.com"}
   {:rodadas_id  "3"
    :user        "Hector Lucero"
    :comentarios "Alli estaremos"
    :email       "hectorqlucero@gmail.com"}
   {:rodadas_id  "4"
    :user        "Martha Lucero"
    :comentarios "Alli estaremos"
    :email       "marthalucero56@gmail.com"}
   {:rodadas_id  "4"
    :user        "Hector Lucero"
    :comentarios "Alli estaremos"
    :email       "hectorqlucero@gmail.com"}
   {:rodadas_id  "8"
    :user        "Martha Lucero"
    :comentarios "Alli estaremos"
    :email       "marthalucero56@gmail.com"}
   {:rodadas_id  "8"
    :user        "Hector Lucero"
    :comentarios "Alli estaremos"
    :email       "hectorqlucero@gmail.com"}])

(def user-rows
  [{:lastname  "user"
    :firstname "admin"
    :username  "admin"
    :password  (crypt/encrypt "admin")
    :dob       "1957-02-07"
    :level     "A"
    :active    "T"}
   {:lastname  "user"
    :firstname "regular"
    :username  "user"
    :password  (crypt/encrypt "user")
    :dob       "1956-02-23"
    :level     "U"
    :active    "T"}
   {:lastname  "Lucero"
    :firstname "Hector"
    :username  "hectorqlucero@gmail.com"
    :password  (crypt/encrypt "elmo1200")
    :dob       "1957-02-07"
    :level     "A"
    :active    "T"}
   {:lastname  "Lucero"
    :firstname "Martha"
    :username  "marthalucero56@gmail.com"
    :dob       "1956-02-23"
    :password  (crypt/encrypt "preciosa")
    :level     "U"
    :active    "T"}])

(defn create-database []
  "Creates database and a default admin user"
  (Query! db users-sql)
  (Query! db cuadrantes-sql)
  (Query! db rodadas-sql)
  ;;(Query! db rodadas_link-sql)
  (Insert-multi db :users user-rows)
  (Insert-multi db :rodadas rodadas-rows)
  (Insert-multi db :cuadrantes cuadrantes-rows))

(defn reset-database []
  "removes existing tables and recreates them"
  (Query! db "DROP table IF EXISTS users")
  (Query! db "DROP table IF EXISTS cuadrantes")
  (Query! db "DROP table IF EXISTS rodadas_link")
  (Query! db "DROP table IF EXISTS rodadas")
  (Query! db users-sql)
  (Query! db cuadrantes-sql)
  (Query! db rodadas-sql)
  (Query! db rodadas_link-sql)
  (Insert-multi db :users user-rows)
  (Insert-multi db :cuadrantes cuadrantes-rows)
  (Insert-multi db :rodadas rodadas-rows)
  (Insert-multi db :rodadas_link rodadas_link-rows))

(defn migrate []
  "migrate by the seat of my pants"
  (Query! db "DROP table IF EXISTS rodadas_link")
  (Query! db "DROP table IF EXISTS rodadas")
  (Query! db rodadas-sql)
  (Query! db rodadas_link-sql)
  (Insert-multi db :rodadas rodadas-rows)
  (Insert-multi db :rodadas_link rodadas_link-rows))
(migrate)
