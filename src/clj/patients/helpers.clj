(ns patients.helpers)

(defn date-to-string [date]
  (->>
   (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd")
   (.format date)))

(defn string-to-date [date-string]
  (try (->> date-string
            (java.time.LocalDate/parse))
       (catch Exception _ date-string)))

(defn parse-int [s]
  (try (Integer/parseInt (re-find #"\A-?\d+" s))
       (catch Exception _ s)))

(comment (date-to-string (string-to-date "2018-01-01"))
         (parse-int nil))
