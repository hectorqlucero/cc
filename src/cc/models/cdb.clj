(ns cc.models.cdb
  (:require [cc.models.crud :refer :all]
            [noir.util.crypt :as crypt]))

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
  level char(1) DEFAULT NULL COMMENT 'A=Administrador,U=Usuario,S=Sistema',
  active char(1) DEFAULT NULL COMMENT 'T=Active,F=Not active',
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
  descripcion_corta varchar(100) DEFAULT NULL,
  descripcion varchar(3000) DEFAULT NULL,
  punto_reunion varchar(1000) DEFAULT NULL,
  nivel char(1) DEFAULT NULL COMMENT 'P=Principiantes,M=Medio,A=Avanzado,T=Todos',
  distancia varchar(100) DEFAULT NULL,
  velocidad varchar(100) DEFAULT NULL,
  fecha date DEFAULT NULL,
  hora time DEFAULT NULL,
  leader varchar(100) DEFAULT NULL,
  leader_email varchar(100) DEFAULT NULL,
  cuadrante int(11) DEFAULT NULL,
  repetir char(1) DEFAULT NULL COMMENT 'T=Si,F=No',
  anonimo char(1) DEFAULT \"F\" COMMENT 'T=Si,F=No',
  PRIMARY KEY (id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8")

(def rodadas_link-sql
  "CREATE TABLE rodadas_link (
  id int(11) NOT NULL AUTO_INCREMENT,
  rodadas_id int(11) NOT NULL,
  user varchar(200) DEFAULT NULL,
  comentarios TEXT DEFAULT NULL,
  email varchar(100) DEFAULT NULL,
  asistir char(1) DEFAULT \"T\" COMMENT 'T=Si,F=No',
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
    :status       "T"}
   {:name "Grupo Ciclista La Vid"
    :leader "Marco Romero"
    :leader_email "mromeropmx@hotmail.com"
    :notes "Un grupo cristiano con deseos de mejorar nuestra salud..."
    :status "T"}
   {:name         "Reto Demoledor"
    :leader       "Reto"
    :leader_email "retodemoledor@server.com"
    :notes        "Para mas información entra al grupo Reto Demoledor"
    :status       "T"}
   {:name "Reto Aerobiker"
    :leader "retoaerobiker"
    :leader_email "retoaerobiker@server.com"
    :notes "Reto madrugador 5x5 mas detalles en Aerobikers (FB)"
    :status "T"}
   {:name         "Blanco"
    :leader       "blancolider"
    :leader_email "blancolider@server.com"
    :notes        ""
    :status       "T"}
   {:name         "Bicios@s"
    :leader       "biciosolider"
    :leader_email "biciosolider@server.com"
    :notes        ""
    :status       "T"}
   {:name         "AeroGreens"
    :leader       "aerogreens"
    :leader_email "aerogreens@server.com"
    :notes        ""
    :status       "T"}
   {:name         "Akalambrados"
    :leader       "Frank"
    :leader_email "frank@akalambrados.com"
    :notes        ""
    :status       "T"}
   {:name         "V-Light"
    :leader       "vlight"
    :leader_email "vlight@server.com"
    :notes        ""
    :status       "T"}
   {:name         "Aferreitorxs"
    :leader       "aferreitorxslider"
    :leader_email "aferreitorxs@server.com"
    :notes        ""
    :status       "T"}
   {:name         "Mujeres al Pedal"
    :leader       "mujeres"
    :leader_email "mujeres@server.com"
    :notes        ""
    :status       "T"}
   {:name         "Raptors"
    :leader       "raptors"
    :leader_email "raptors@server.com"
    :notes        ""
    :status       "T"}
   {:name         "Victorianos"
    :leader       "victorianos"
    :leader_email "victorianos@server.com"
    :notes        ""
    :status       "T"}
   {:name         "I. V. Cycling (Inter)"
    :leader       "ivcycling"
    :leader_email "ivcycling@server.com"
    :notes        ""
    :status       "T"}
   {:name         "NONSTOP"
    :leader       "nonstop"
    :leader_email "nonstop@server.com"
    :notes        ""
    :status       "T"}
   ])

(def rodadas-rows
  [{:descripcion_corta "San Lunes"
    :descripcion       "Ruta que puede variar por las calles de la ciudad. Se rodaran por lo menos 20 kilometros.  No olvidar traer casco, luces, auga y un tubo de repuesto."
    :punto_reunion     "Parque Hidalgo"
    :nivel             "P"
    :distancia         "20/28 Km"
    :velocidad         "18-25Km/hr"
    :fecha             "2018-10-08"
    :hora              "20:00:00"
    :leader            "Christian"
    :leader_email      "hectorqlucero@gmail.com"
    :cuadrante         1
    :repetir           "T"
    :anonimo           "F"}
   {:descripcion_corta "Santa Isabel"
    :descripcion       "Salimos del Parque Hidalgo hacia la Santa Isabel.  Hidratacion en la OXXO que esta en la Lazaro Cardenas.  No olividen traer casco, luces, agua y un tubo de repuesto."
    :punto_reunion     "Parque Hidalgo"
    :nivel             "T"
    :distancia         "30 Km"
    :velocidad         "Lights: 18-25Km/hr  Intermedios: 25-35Km/hr"
    :fecha             "2018-10-09"
    :hora              "20:00:00"
    :leader            "Ruth"
    :leader_email      "hectorqlucero@gmail.com"
    :cuadrante         1
    :repetir           "T"
    :anonimo           "F"}
   {:descripcion_corta "Canalera"
    :descripcion       "Salimos por el canal de la Independencia a veces hasta el aeropuerto.  No olviden traer casco, luces, agua y un tubo de repuesto."
    :punto_reunion     "Parque Hidalgo"
    :nivel             "M"
    :distancia         "30/50Km"
    :velocidad         "25-35Km/hr"
    :fecha             "2018-10-08"
    :hora              "20:00:00"
    :leader            "Humberto"
    :leader_email      "hectorqlucero@gmail.com"
    :cuadrante         1
    :repetir           "T"
    :anonimo           "F"}
   {:descripcion_corta "Adorada"
    :descripcion       "Ruta que puede variar entre el Campestre y el Panteon rumbo al aeropuerto.  No olviden traer casco, luces, agua y un tubo de repuesto."
    :punto_reunion     "Parque Hidalgo"
    :nivel             "P"
    :distancia         "20/28 Km"
    :velocidad         "18-25Km/hr"
    :fecha             "2018-10-10"
    :hora              "20:00:00"
    :leader            "Martha Parada"
    :leader_email      "hectorqlucero@gmail.com"
    :cuadrante         1
    :repetir           "T"
    :anonimo           "F"}
   {:descripcion_corta "Vida Libre"
    :descripcion       "5to paseo recreativo Juntos por una VIDA LIBRE de Cancer de MAMA
Viste una prenda rosa y cuelga un liston del mismo color en tu bicicleta.
Rueda, corre, trota camina
Por tu seguridad no olvides tu casco
CIRCUITO PEDESTRES 2 KM
CIRCUITO CICLISTAS 4 KM
Registro: 7:30am.   Salida: 8:00am"
    :punto_reunion     "Oficinas Centrales (Calle Calafia #1115)"
    :nivel             "T"
    :distancia         "4 Km"
    :velocidad         "5-25Km/hr"
    :fecha             "2018-10-21"
    :hora              "08:00:00"
    :leader_email      "hectorqlucero@gmail.com"
    :repetir           "F"
    :anonimo           "F"}
   {:descripcion_corta "Circuito Obregon"
    :descripcion       "Hoy toca Circuito Ciclista Obregón los esperamos a las 8:00 PM en el punto de Reunión de Rectoría de la UABC/Biblioteca del Estado. El circuito tiene una longuitud de 3.2 Kilómetros con muy buena iluminado y el formato que se manejara para rodar será de las primeras 9 vueltas serán controladas a 30 kilómetros por hora como máximo y después se comenzara a aumentar la velocidad. Los ciclistas que lleguen más tarde se pueden acoplar al grupo o grupos. Así que a rodar con precaución y llevar sus luces si cuentan con ellas para iluminar esa mancha ciclista por toda la Av. Obregón, A Darle…."
    :punto_reunion     "Rectoría de la UABC/Biblioteca del Estado"
    :nivel             "T"
    :distancia         "20/50 Km"
    :velocidad         "10-40Km/hr"
    :fecha             "2018-10-24"
    :hora              "20:00:00"
    :leader            "Melissa Utsler"
    :leader_email      "hectorqlucero@gmail.com"
    :cuadrante         1
    :repetir           "T"
    :anonimo           "F"}
   {:descripcion_corta "Culinaria"
    :descripcion       "Salimos del parque Hidalgo hacia el panteon que esta rumbo al aeropuerto. No olviden traer casco, luces, agua y un tubo de repuesto."
    :punto_reunion     "Parque Hidalgo"
    :nivel             "P"
    :distancia         "20/28 Km"
    :velocidad         "18-25Km/hr"
    :fecha             "2018-10-11"
    :hora              "20:00:00"
    :leader            "Chefsito"
    :leader_email      "hectorqlucero@gmail.com"
    :cuadrante         1
    :repetir           "T"
    :anonimo           "F"}
   {:descripcion_corta "Intermedia"
    :descripcion       "Salimos del parque Hidalgo hacia el hotel que esta despues del OXXO.  No olviden traer casco, luces, agua y un tubo de repuesto."
    :punto_reunion     "Parque Hidalgo"
    :nivel             "M"
    :distancia         "30 Km"
    :velocidad         "25-35 km/hr"
    :fecha             "2018-10-11"
    :hora              "20:00:00"
    :leader            "Humberto"
    :leader_email      "hectorqlucero@gmail.com"
    :cuadrante         1
    :repetir           "T"
    :anonimo           "F"}
   {:descripcion_corta "Pedacera"
    :descripcion       "Salimos del parque Hidalgo hacia el aeropuerto.  No olviden traer casco, luces, agua y un tubo de repuesto."
    :punto_reunion     "Parque Hidalgo"
    :nivel             "A"
    :distancia         "50-60 Km"
    :velocidad         "30-40Km/hr"
    :fecha             "2018-10-11"
    :hora              "20:00:00"
    :leader            "Oscar Raul"
    :leader_email      "hectorqlucero@gmail.com"
    :cuadrante         1
    :repetir           "T"
    :anonimo           "F"}
   {:descripcion_corta "Familiar"
    :descripcion       "Salimos del Parque Hidalgo con ruta indefinida.  No olviden traer casco, luces, agua y un tubo de repuesto."
    :punto_reunion     "Parque Hidalgo"
    :nivel             "P"
    :distancia         "20/28 Km"
    :velocidad         "18-25Km/hr"
    :fecha             "2018-10-12"
    :hora              "20:00:00"
    :leader            "Jose el Pechocho"
    :leader_email      "hectorqlucero@gmail.com"
    :cuadrante         1
    :repetir           "T"
    :anonimo           "F"}
   {:descripcion_corta "Mexicali Rumorosa"
    :descripcion       "Paseo Ciclista Mexicali-Rumorosa, es el Paseo ciclista mas impresionante en la franja fronteriza entre Mexicali B.C. y Estados Unidos por su vista espectacular natural, donde el participante se deleitara al observar paisajes de magnificas montañas conformadas por un increíble escenario de rocas gigantescas con impresionantes miradores, lo que caracteriza la subida de la carretera a la Rumorosa entre Mexicali y Tecate B.C. 
Un Paseo divertido, familiar de sana convivencia para todos los que gustan de nuevos retos.

Una vez en la meta recibiremos a todos los participantes y sus Familia en una Convivencia en la Rumorosa donde además de pasar un rato agradable recibirán su Medalla por participar y se realiza una Rifa de 2 Bicicletas entre los Participantes."
    :punto_reunion "Macroplaza del Valle Lázaro Cárdenas 2200 col. El Porvenir, 21220 Mexicali, Baja California."
    :nivel         "T"
    :distancia     "75 Km"
    :fecha         "2018-10-27"
    :hora          "08:00:00"
    :leader        "Martha Parada"
    :leader_email  "hectorqlucero@gmail.com"
    :repetir       "F"
    :anonimo       "F"
    :velocidad     "15-40Km/hr"}
   {:descripcion_corta "VI Gran Fondo"
    :descripcion       "Desafiando tus piernas
VI Gran Fondo
Paseo Mexicali - San Felipe
Distancia: 200 kms de diversión
Ven a disfrutar de este paseo donde pondrás a prueba tus piernas.
Costo: $300 pesos
Contamos con ambulancia, hidrataciones, medalla y el mejor ambiente al final del evento"
    :punto_reunion     "Parque Hidalgo"
    :nivel             "A"
    :distancia         "200 Km"
    :fecha             "2018-11-10"
    :hora              "06:00:00"
    :leader            "Rosy Rutiaga"
    :leader_email      "hectorqlucero@gmail.com"
    :repetir           "F"
    :anonimo           "F"
    :velocidad         "15-40Km/hr"}
   {:descripcion_corta "San Luis al Golfo"
    :descripcion       "5to paseo anual 360 Cycling Studio.
San Luis - El Golfo, 112 kilometros.
Inscripcion $550 pesos
Barredora
puntos de hidratacion
chip de cronometraje
Camiseta
Comida
Bebida
Medalla de Participacion
Transporte Golfo - San Luis
Informes: (653) 103-1460 * (653) 119-0725"
    :punto_reunion     "360 Cycling Studio, San Luis R.C."
    :nivel             "T"
    :distancia         "120 Km"
    :velocidad         "15-40Km/hr"
    :fecha             "2018-11-10"
    :hora              "07:00:00"
    :leader            ""
    :leader_email      "hectorqlucero@gmail.com"
    :repetir           "F"
    :anonimo           "F"}
   ])

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
  [{:lastname  "Lucero"
    :firstname "Hector"
    :username  "hectorqlucero@gmail.com"
    :password  (crypt/encrypt "elmo1200")
    :dob       "1957-02-07"
    :email     "hectorqlucero@gmail.com"
    :level     "S"
    :active    "T"}
   {:lastname  "Hernandez"
    :firstname "Oscar"
    :username  "rarome93@gmail.com"
    :password  (crypt/encrypt "oscarhernandez")
    :dob       "1975-10-08"
    :email     "rarome93@gmail.com"
    :level     "S"
    :active    "T"}
   {:lastname  "Romero"
    :firstname "Marco"
    :username  "mromeropmx@hotmail.com"
    :password  (crypt/encrypt "marcoromero")
    :dob       "1975-03-06"
    :email     "mromeropmx@hotmail.com"
    :level     "S"
    :active    "T"}
   {:lastname  "Lucero"
    :firstname "Martha"
    :username  "marthalucero56@gmail.com"
    :dob       "1956-02-23"
    :email     "marthalucero56@gmail.com"
    :password  (crypt/encrypt "preciosa")
    :level     "U"
    :active    "T"}])

(defn create-database []
  "Creates database and a default admin user"
  (Query! db users-sql)
  (Query! db cuadrantes-sql)
  (Query! db rodadas-sql)
  (Query! db rodadas_link-sql)
  (Insert-multi db :users user-rows)
  (Insert-multi db :rodadas rodadas-rows)
  (Insert-multi db :rodadas_link rodadas_link-rows)
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
;;(reset-database)
