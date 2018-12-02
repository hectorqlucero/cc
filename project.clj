(defproject cc "0.1.0"
  :description "Skeleton for Web app"
  :url "http://127.0.0.1:8080"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [clojure-csv/clojure-csv "2.0.2"]
                 [compojure "1.6.1"]
                 [lib-noir "0.9.9"]
                 [com.draines/postal "2.0.3"]
                 [cheshire "5.8.1"]
                 [clj-pdf "2.2.33" :exclusions [commons-codec]]
                 [pdfkit-clj "0.1.7"]
                 [clj-time "0.15.1"]
                 [date-clj "1.0.1"]
                 [clojurewerkz/money "1.10.0"]
                 [org.clojure/java.jdbc "0.7.8"]
                 [org.clojure/data.codec "0.1.1"]
                 [mysql/mysql-connector-java "8.0.13"]
                 [selmer "1.12.5" :exclusions [commons-codec]]
                 [inflections "0.13.0" :exclusions [commons-codec]]
                 [ring/ring-devel "1.7.1"]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-anti-forgery "1.3.0"]
                 [ring/ring-defaults "0.3.2"]]
  :template-additions [".gitignore" "README.md"]
  :jvm-opts ["-Duser.timezone=UTC"]
  :main ^:ccip-aot cc.core
  :aot [cc.core]
  :uberjar-name "cc.jar"
  :plugins [[lein-ancient "0.6.10"]
            [lein-pprint "1.1.2"]]
  :ring {:handler cc.core/app
         :auto-reload? true
         :auto-refresh? false}
  :target-path "target/%s"
  :resources-paths ["shared" "resources"]
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]}})
