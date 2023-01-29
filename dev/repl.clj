(ns repl
  (:require [shadow.cljs.devtools.server :as server]
            [shadow.cljs.devtools.api :as shadow]))

(server/start!)
; (shadow/compile :app)
(shadow/watch :app)

