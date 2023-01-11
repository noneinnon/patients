all:
	source .env && sh ./echo_env.sh

repl:
	make server & make front

server:
	make all & clj -M:repl/conjure

front:
	npx shadow-cljs watch app

run_test: 
	DB_NAME="patients_test" make all & clj -M:test

