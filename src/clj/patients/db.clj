(ns patients.db
  (:require [patients.env :refer [env]]
            [honey.sql :as sql]
            [honey.sql.pg-ops :as pg-ops]
            [honey.sql.helpers :as h]
            ;; https://clojure-doc.org/articles/ecosystem/java_jdbc/home/
            [clojure.java.jdbc :as j]
            [ragtime.jdbc :as jdbc]
            [clojure.string :refer [join]]
            [ragtime.repl :as repl]))

;; config
(def pg-db {:dbtype "postgresql"
            :dbname (env :DB_NAME)
            :host (env :DB_HOST)
            :port (env :DB_PORT)
            :user (env :DB_USER)
            :password (env :DB_PASSWORD)})

;; migrations
;; TODO make migrations work
; (defn create-config
;   "creates a config map for ragtime"
;   []
;   {:datastore (jdbc/sql-database pg-db)
;    :migrations (jdbc/load-resources "migrations")})

; (defn migrate []
;   (repl/migrate (create-config)))
;
; (defn rollback []
;   (repl/rollback (create-config)))

;; CRUD
(defn query [q]
  (j/query pg-db q))

(defn get-patients [{:keys [first_name
                            last_name
                            id
                            age
                            address
                            insurance_number
                            sort-param
                            order
                            offset
                            limit]}]
  (query (-> (h/select :*)
             (h/from :patients)
             (h/where :and
                      (when id [:= :patients.id id])
                      (when first_name [pg-ops/iregex :patients.first_name first_name])
                      (when last_name [pg-ops/iregex :patients.last_name last_name])
                      (when address [pg-ops/iregex :patients.address address])
                      (when age [:= :patients.age age])
                      (when insurance_number [:= :patients.age insurance_number]))
             (h/order-by [sort-param order])
             (h/offset offset)
             (h/limit limit)
             (sql/format))))

(defn create-patient [patient]
  (j/insert! pg-db :patients patient))

(defn update-patient [id updates]
  (j/update! pg-db :patients updates ["id = ?" id]))

(defn delete-patient [id]
  (j/delete! pg-db :patients ["id = ?" id]))

