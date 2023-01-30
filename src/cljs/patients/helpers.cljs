(ns patients.helpers)

(def log (.-log js/console))

(defn form-get [form key]
  (.get form key))

; (log (.. js/window -location -search))

(defn get-patient-form-data [form]
  {:first_name (form-get form "first_name")
   :last_name (form-get form "last_name")
   :sex (form-get form "sex")
   :age (form-get form "age")
   :dob (form-get form "dob")
   :insurance_number (form-get form "insurance_number")
   :address (form-get form "address")})

