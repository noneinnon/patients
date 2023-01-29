(ns clj.patients.core-test
  (:require [patients.core :as patients]
            [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [clojure.java.jdbc :as j]
            [muuntaja.core :as m]
            [patients.helpers :as helpers]
            [patients.db :as db]
            [kaocha.repl :as k]
            [kaocha.watch :as w]))

;; helpers
(defn parse-body [request]
  (let [body (:body request)]
    (assoc request :body (m/decode "application/json" body))))

;; setup

; Migrations must be applied as well
;--------------------------------------------------
; fixuture hooks
(def create-db "create table if not exists patients (
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

(def patients-fixture [{:first_name "john" :last_name "lennon" :age 40 :sex "m" :insurance_number "1234567890123" :address "Liverpool, Penny Lane, 8" :dob (helpers/string-to-date "1940-10-09")} {:first_name "paul" :last_name "mccartney" :age 40 :sex "m" :insurance_number "1234567890123" :address "Liverpool, Penny Lane, 8" :dob (helpers/string-to-date "1940-10-09")} {:first_name "george" :last_name "harrison" :age 40 :sex "m" :insurance_number "1234567890123" :address "Liverpool, Penny Lane, 8" :dob (helpers/string-to-date "1940-10-09")}])

(defn setup []
  (j/db-do-commands db/pg-db [create-db])
  (j/insert-multi! db/pg-db :patients patients-fixture))

(defn clean-up []
  (j/db-do-commands db/pg-db "delete from patients;"))
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
(deftest index
  (testing "should return body with html")
  (let [response (-> (mock/request :get "/") patients/app)
        body (:body response)
        headers (:headers response)]
    (and (is (= (:status response) 200))
         (is (= headers {"content-type" "text/html"}))
         (not (= (count body) 0)))))

(deftest get-all-patients
  (testing "should return patient list")
  (let [response (-> (mock/request :get "/api/patients" {:sort-param "first_name" :order "asc"})
                     patients/app
                     parse-body)
        [patients total] (:body response)]
    (and (is (= (:status response) 200))
         (is (= total 3))
         (is (= (:first_name (first patients)) "george")))))

(deftest get-filtered-patients
  (testing "correctly filters out")
  (let [response (-> (mock/request :get "/api/patients" {:first_name "jo"})
                     patients/app
                     parse-body)
        [_ total] (:body response)]
    (and (is (= (:status response) 200))
         (is (= total 1)))))

(deftest create-patient
  (testing "should correctly create new patient")
  (let [patient {:first_name "ringo" :last_name "starr" :age "40" :sex "m" :insurance_number "1234567890123" :address "Liverpool, Penny Lane, 8" :dob "1940-10-09"}
        response (-> (mock/request :post "/api/patients" patient) patients/app parse-body)
        created (first (:body response))
        by-id-response (-> (mock/request :get (str "/api/patients/" (:id created)))
                           patients/app
                           parse-body
                           :body)]
    (prn created)
    (and (is (= (:status response) 201))
         (is (= by-id-response (:body response))))))

(deftest create-patient-failure
  (testing "should return 400 with errors")
  (let [patient {:first_name "1234" :last_name "54321" :age "asd" :sex "l" :insurance_number "123456789a" :address "Liverpool, Penny Lane, 8" :dob "1940-99-09"}
        response (-> (mock/request :post "/api/patients" patient) patients/app parse-body)]
    (and (is (= (:status response) 400))
         (is (= true true)))))

(deftest error-by-id
  (testing "should return 400")
  (let [response (-> (mock/request :get (str "/api/patients/" 0)) patients/app)]
    (is (= (:status response) 400))))

(deftest update-patient
  (testing "should correctly update patient")
  (let [[patients] (-> (mock/request :get "/api/patients")
                       patients/app
                       parse-body
                       :body)
        id (:id (first patients))
        update-response (-> (mock/request :put (str "/api/patients/" id) {"first_name" "paul", "last_name" "mccartney", "sex" "m", "age" "40", "dob" "2020-10-24", "insurance_number" "1234567890123", "address" "Liverpool, Penny Lane, 8", "id" "659"}) patients/app parse-body)]
    (prn (:body update-response))
    (is (= (:body update-response) [1]))))

(deftest update-patient-failure
  (testing "should return 400 with errors")
  (let [[patients] (-> (mock/request :get "/api/patients")
                       patients/app
                       parse-body
                       :body)
        id (:id (first patients))
        update-response (-> (mock/request :put (str "/api/patients/" id) {:first_name 123}) patients/app parse-body)]
    (and
     (is (= (:status update-response) 400))
     (is (= (empty? (:body update-response)) false)))))

(deftest delete-patients
  (testing "should correctly delete patient")
  (let [[patients] (-> (mock/request :get "/api/patients")
                       patients/app
                       parse-body
                       :body)
        id (:id (first patients))
        delete-response (-> (mock/request :delete (str "/api/patients/" id)) patients/app parse-body)]
    (is (= (:body delete-response) [1]))))

(comment (w/run (k/config)))
