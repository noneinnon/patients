(ns patients.env)

(defn env [key]
  (System/getenv (name key)))

(comment
  (name :DB_NAME)
  (env :DB_NAME)
  (System/getenv "DB_NAME"))
