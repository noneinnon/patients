echo-env:
	sh ./echo_env.sh

repl:
	npx env-cmd clj -M:repl/conjure

shadow:
	npm run tailwind:watch

run_test: 
	npx env-cmd --file .env.test clj -M:test

build-clj:
	clj -T:build uber

build-cljs:
	npx shadow-cljs release app

build:
	npm ci && make build-cljs && make build-clj
