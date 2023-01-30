(ns patients.db
  (:require [patients.env :refer [env]]
            [honey.sql :as sql]
            [honey.sql.pg-ops :as pg-ops]
            [honey.sql.helpers :as h]
            ;; https://clojure-doc.org/articles/ecosystem/java_jdbc/home/
            [clojure.java.jdbc :as j]))

;; config
(def pg-db {:dbtype "postgresql"
            :dbname (env :DB_NAME)
            :host (env :DB_HOST)
            :port (env :DB_PORT)
            :user (env :DB_USER)
            :password (env :DB_PASSWORD)})

(def create-table "create table if not exists patients (
                                id serial primary key, 
                                first_name varchar(255) not null,
                                last_name varchar(255) not null,
                                age int not null,
                                sex varchar(1) not null,
                                dob date not null,
                                insurance_number varchar(70) not null,
                                address varchar(500) not null,
                                createdAt timestamp not null default now(),
                                updatedAt timestamp not null default now());")
;
; (j/db-do-commands pg-db [(format "create database if not exists %s;" (env :DB_NAME))])
;
; (j/db-do-commands pg-db [create-table])

;; CRUD
(defn query [q]
  (j/query pg-db q))

(defn get-patient-data [data]
  (select-keys data [:first_name :last_name :age :dob :sex :address :insurance_number]))

;; https://cljdoc.org/d/com.github.seancorfield/honeysql/2.4.969/doc/getting-started
(defn get-patients [{:keys [first_name
                            last_name
                            age
                            id
                            address
                            insurance_number
                            sort-param
                            order
                            offset
                            limit]}]
  (let [where #(h/where % :and
                        (when id [:= :patients.id id])
                        (when first_name [pg-ops/iregex :patients.first_name first_name])
                        (when last_name [pg-ops/iregex :patients.last_name last_name])
                        (when address [pg-ops/iregex :patients.address address])
                        (when age [:= :patients.age age])
                        (when insurance_number [:= :patients.insurance_number insurance_number]))
        patients (query (-> (h/select :*)
                            (h/from :patients)
                            (where)
                            (h/order-by [sort-param order])
                            (h/offset offset)
                            (h/limit limit)
                            (sql/format)))
        [{total :total}] (query (-> (h/select [:%count.* "total"])
                                    (h/from :patients)
                                    (where)
                                    (sql/format)))]
    [patients total]))

(defn create-patient [patient]
  (j/insert! pg-db :patients (get-patient-data patient)))

(defn update-patient [id updates]
  (j/update! pg-db :patients (get-patient-data updates) ["id = ?" id]))

(defn delete-patient [id]
  (j/delete! pg-db :patients ["id = ?" id]))

(comment
  (first (get-patients {:sort-param :first_name :order :asc :offset 0 :limit 1}))
  (query (-> (h/select :first_name)
             (h/from :patients)
             (h/order-by "first_name")
             (sql/format))))
