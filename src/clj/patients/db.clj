(ns patients.db
  (:require [patients.env :refer [env]]
            [honey.sql :as sql]
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
(defn create-config
  "creates a config map for ragtime"
  []
  {:datastore (jdbc/sql-database pg-db)
   :migrations (jdbc/load-resources "migrations")})

(defn migrate []
  (repl/migrate (create-config)))

(defn rollback []
  (repl/rollback (create-config)))

;; CRUD
(defn query [q]
  (j/query pg-db q))

(defn get-patients [{where :where order-by :order-by}]
  (query (-> (h/select :*)
             (h/from :patients)
             (h/where where)
             (h/order-by (or order-by :id))
             (sql/format))))

(defn create-patient [patient]
  (j/insert! pg-db :patients patient))

(defn update-patient [id updates]
  (j/update! pg-db :patients updates ["id = ?" id]))

(defn delete-patient [id]
  (j/delete! pg-db :patients ["id = ?" id]))

(comment
  (get-patients {:where [:= :patients.id 388]})
  (get-patients {})

  (query (-> (h/select :*)
             (h/from :adress)
             (sql/format {:pretty true})))

  (query (-> (h/select :*)
             (h/from :patients)
             (sql/format)))

  (-> {:insert-into [:patients]
       :columns [:sex :insurance_number :adress_id]
       :values [["M" "1234567890777" 2]]}
      (sql/format {:pretty true}))
  (-> (h/insert-into :patients_test)
      (h/values [{:first_name "asdasd"}])
      (sql/format)
      join)

  (migrate)
  (rollback)
  (j/db-do-commands pg-db "create table if not exists patients (
                                id serial primary key, 
                                first_name varchar(255) not null,
                                last_name varchar(255) not null,
                                age int not null,
                                sex varchar(1) not null,
                                dob date not null,
                                insurance_number varchar(70) not null,
                                address varchar(500) not null,
                                createdAt timestamp not null default now(),
                                updatedAt timestamp not null default now());"))
