all:
	source .env & sh ./echo_env.sh

repl:
	 make all & clj -M:repl/conjure

run_test: 
	DB_NAME="patients_test" make all & clj -M:test

front:
	npx shadow-cljs watch app
