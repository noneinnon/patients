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

psql:
	docker-compose exec db psql -U admin

build-app:
	docker-compose -f docker-compose.prod.yml build --no-cache app

start:
	docker-compose -f docker-compose.prod.yml up --build --force-recreate --remove-orphans -d

start-app:
	docker-compose -f docker-compose.prod.yml up --build --force-recreate --remove-orphans -d app

apply-migrations:
	npx env-cmd clj -X:apply-migrations

rollback-migrations:
	npx env-cmd clj -X:rollback-migrations

exec:
	docker-compose exec app /bin/bash

logs:
	docker-compose logs -f app

restart:
	docker-compose restart app

stop:
	docker-compose stop

