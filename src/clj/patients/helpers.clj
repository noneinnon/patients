(ns patients.helpers)

(defn date-to-string [date]
  (->> (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd")
       (.format date)))

(defn string-to-date [date-string]
  (->> date-string
       (java.time.LocalDate/parse)))

(defn parse-int [s]
  (Integer/parseInt (re-find #"\A-?\d+" s)))

(comment (date-to-string (string-to-date "2018-01-01"))
         (parse-int "40"))
