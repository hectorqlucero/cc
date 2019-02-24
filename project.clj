(defproject cc "0.1.0"
  :description "Skeleton for Web app"
  :url "http://127.0.0.1:8080"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [clojure-csv/clojure-csv "2.0.2"]
                 [compojure "1.6.1" :exclusions [commons-codec]]
                 [lib-noir "0.9.9"]
                 [com.draines/postal "2.0.3"]
                 [cheshire "5.8.1"]
                 [clj-pdf "2.3.1" :exclusions [commons-codec]]
                 [pdfkit-clj "0.1.7" :exclusions [commons-codec commons-logging]]
                 [clj-time "0.15.1"]
                 [date-clj "1.0.1"]
                 [clojurewerkz/money "1.10.0"]
                 [org.clojure/java.jdbc "0.7.9"]
                 [org.clojure/data.codec "0.1.1"]
                 [mysql/mysql-connector-java "8.0.15"]
                 [selmer "1.12.8" :exclusions [commons-codec]]
                 [inflections "0.13.2" :exclusions [commons-codec]]
                 [ring/ring-devel "1.7.1" :exclusions [commons-codec ring/ring-codec]]
                 [ring/ring-core "1.7.1" :exclusions [commons-codec ring/ring-codec]]
                 [ring/ring-anti-forgery "1.3.0"]
                 [ring/ring-defaults "0.3.2"]]
  :template-additions [".gitignore" "README.md"]
  :jvm-opts ["-Duser.timezone=UTC"]
  :main ^:ccip-aot cc.core
  :aot [cc.core]
  :uberjar-name "cc.jar"
  :plugins [[lein-pprint "1.2.0"]]
  :ring {:handler cc.core/app
         :auto-reload? true
         :auto-refresh? false}
  :target-path "target/%s"
  :resources-paths ["shared" "resources"]
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]}})
