(ns server.db
  (:require [server.env :refer [env]]
            ; [clj-time.core :as t]
            ; [clj-time.format :as f]
            ; [clj-time.jdbc]
            [honey.sql :as sql]
            [honey.sql.helpers :as h]
            [clojure.java.jdbc :as j]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]
            [server.helpers :refer [string-to-date]]))

;; config

(def pg-db {:dbtype "postgresql"
            :dbname (env :DB_NAME)
            :host (env :DB_HOST) :port (env :DB_PORT)
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
  (println str order-by)
  (query (-> (h/select :*)
             (h/from :patients)
             (h/where where)
             (h/order-by (or order-by :id))
             (sql/format))))

(defn insert-patients [& patients]
  (query (-> (h/insert-into :patients)
             (h/values patients)
             (sql/format))))

(defn remove-patient [patient]
  (query (-> (h/delete :patients)
             (h/where {:id (:id patient)})
             (sql/format))))

(comment
  (get-patients {})

  (query (-> (h/select :*)
             (h/from :adress)
             (sql/format {:pretty true})))

  (query (-> (h/select :*)
             (h/from :patients)
             (sql/format)))

  (query (-> {:insert-into [:adress]
              :columns [:country :city :street :house]
              :values [["Germany" "Berlin" "Kurfürstendamm" "1"]
                       ["England" "London" "Baker str" "14"]]}
             (sql/format {:pretty true})))

  ; (query (-> {:insert-into [:patients]
  ;             :columns [:sex :dob :insurance_number :adress_id]
  ;             :values [["M" (t/date-time 1983 10 14) "1234567890777" 2]]}
  ;            (sql/format {:pretty true})))

  (migrate)
  (rollback)
  (query "CREATE TABLE adress (
                             id serial primary key,
                             country varchar(100) not null,
                             city varchar(100) not null,
                             street varchar(255) not null,
                             house varchar(100) not null,
                             createdAt timestamp not null default now(),
                             updatedAt timestamp not null default now()
                             );
        CREATE TABLE patients (
                                id serial primary key, 
                                first_name varchar(255) not null,
                                last_name varchar(255) not null,
                                age int not null,
                                sex varchar(1) not null,
                                dob date not null,
                                insurance_number varchar(70) not null,
                                adress_id int references adress(id) not null,
                                createdAt timestamp not null default now(),
                                updatedAt timestamp not null default now()
                                );"))

