(ns server.core-test
  (:require [server.core :as patients]
            [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [clojure.java.jdbc :as j]
            [server.env :refer [env]]))

;; setup
(def db-spec {:dbtype "postgresql"
              :dbname (env :DB_NAME)
              :host (env :DB_HOST) :port (env :DB_PORT)
              :user (env :DB_USER)
              :password (env :DB_PASSWORD)})

; Migrations must be applied as well
;--------------------------------------------------
; fixuture hooks
(defn setup []
  (j/db-do-commands db-spec "create table if not exists patients (
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

(defn clean-up []
  (j/db-do-commands db-spec "delete from patients;"))
;-------------------------------------------------- 

(defn database-reset-fixture
  "Setup: drop all tables, creates new tables
   Teardown: drop all tables
  SQL schema code has if clauses to avoid errors running SQL code.
  Arguments:
  test-function - a function to run a specific test"
  [test-function]
  (setup)
  (test-function)
  (clean-up))

;; https://practical.li/clojure/testing/unit-testing/fixtures.html
(use-fixtures :each database-reset-fixture)

;; tests
(deftest get-all-patients
  (testing "should return patient list")
  (let [response (-> (mock/request :get "/api/patients") patients/app)
        body (:body response)]
    (and (is (= (:status response) 200))
         (is (= (count body)
                0)))))
