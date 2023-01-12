(ns patients.env)

(defn env [key]
  (System/getenv (name key)))

(comment
  (env :DB_NAME)
  (name :DB_NAME)
  (System/getenv "DB_NAME"))
