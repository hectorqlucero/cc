(ns cc.models.crud
  (:require [clojure.java.jdbc :as j]
            [clojure.java.io :as io]))

(defn get-config []
  (binding [*read-eval* false]
    (read-string (str (slurp (io/resource "private/config.clj"))))))

(def config (get-config))

(def db {:classname                       (:db-class config)
         :subprotocol                     (:db-protocol config)
         :subname                         (:db-name config)
         :user                            (:db-user config)
         :password                        (:db-pwd config)
         :useSSL                          false
         :useTimezone                     true
         :useLegacyDatetimeCode           false
         :serverTimezone                  "UTC"
         :noTimezoneConversionForTimeType true
         :dumpQueriesOnException          true
         :autoDeserialize                 true
         :useDirectRowUnpack              false
         :cachePrepStmts                  true
         :cacheCallableStmts              true
         :cacheServerConfiguration        true
         :useLocalSessionState            true
         :elideSetAutoCommits             true
         :alwaysSendSetIsolation          false
         :enableQueryTimeouts             false
         :character-encoding              "UTF-8"
         :zeroDateTimeBehavior            "CONVERT_TO_NULL"}) ; Database connection

(def SALT "897sdn9j98u98kj")                                ; encryption salt for DB

(def KEY "897sdn9j98u98kjz")

(defn aes-in [value & alias]
  (str "AES_ENCRYPT('" value "','" SALT "')"
       (if (seq alias)
         (str " as " (first alias)))))

(defn aes-out [value & alias]
  (str "AES_DECRYPT('" value "','" SALT "')"
       (if (seq alias)
         (str " as " (first alias)))))

(defn aes-sel [field & alias]
  (str "AES_DECRYPT(" field ",'" SALT "')"
       (if (seq alias)
         (str " as " (first alias)))))

(def phone_macc "(___) ___-____")

(def phone_macc_ext "(___) ___-____ x_____")

(def date_macc "mm/dd/YYYY")

(def n4_macc "____")

(def n5_macc "_____")

(defn cleanup_blanks [v]
  (when-not (clojure.string/blank? v) v))

(defn cleanup_phones [v]
  (when-not (= v phone_macc) v))

(defn cleanup_phones_ext [v]
  (when-not (= v phone_macc_ext) v))

(defn cleanup_dates [v]
  (when-not (= v date_macc) v))

(defn cleanup_n4 [v]
  (when-not (= v n4_macc) v))

(defn cleanup_n5 [v]
  (when-not (= v n5_macc) v))

(defn cleanup [row]
  "Cleanup row - convert maccs or blanks into nil"
  (apply merge
         (for [[k v] row]
           (let [value (and (cleanup_blanks v)
                            (cleanup_phones v)
                            (cleanup_phones_ext v)
                            (cleanup_dates v)
                            (cleanup_n4 v)
                            (cleanup_n5 v))]
             {k value}))))

(defn Query
  "queries database accepts query string"
  [db sql]
  (j/query db sql {:entities (j/quoted \`)}))

(defn Query!
  "queries database accepts query string no return value"
  [db sql]
  (j/execute! db sql {:entities (j/quoted \`)}))

(defn Insert
  "Inserts colums in the specified table"
  [db table row]
  (j/insert! db table (cleanup row) {:entities (j/quoted \`)}))

(defn Insert-multi
  "Inserts multiple rows in specified table"
  [db table rows]
  (j/with-db-transaction [t-con db]
    (j/insert-multi! t-con table rows)))

(defn Update
  "Updates columns in the specified table"
  [db table row where-clause]
  (j/update! db table (cleanup row) where-clause {:entities (j/quoted \`)}))

(defn Delete
  "Deletes columns in a specified table"
  [db table where-clause]
  (j/delete! db table where-clause {:entities (j/quoted \`)}))

(defn Save
  "Updates columns or inserts a new row in the specified table"
  [db table row where-clause]
  (j/with-db-transaction [t-con db]
    (let [result (j/update! t-con table (cleanup row) where-clause {:entities (j/quoted \`)})]
      (if (zero? (first result))
        (j/insert! t-con table (cleanup row) {:entities (j/quoted \`)})
        result))))

(defn get-table-columns [table]
  (let [the-fields (Query db (str "DESCRIBE " table))
        tfields (map #(:field %) the-fields)]
    (into [] tfields)))
