echo-env:
	sh ./echo_env.sh

repl:
	node_modules/.bin/env-cmd clj -M:repl/conjure

shadow:
	npm run tailwind:watch

run_test: 
	node_modules/.bin/env-cmd -f .env.test clj -M:test

build-clj:
	clj -T:build uber

build-cljs:
	npm run tailwind && npx shadow-cljs release app

build:
	npm ci && make build-cljs && make build-clj

psql:
	docker-compose exec db psql -U admin

build-app:
	docker-compose build --no-cache app

start-dev:
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml up --build --force-recreate --remove-orphans -d db

start:
	docker-compose up --build --force-recreate --remove-orphans -d

start-app:
	docker-compose up --build --force-recreate --remove-orphans -d app

apply-migrations:
	node_modules/.bin/env-cmd clj -X:apply-migrations

rollback-migrations:
	node_modules/.bin/env-cmd clj -X:rollback-migrations

exec:
	docker-compose exec app /bin/bash

logs:
	docker-compose logs -f app

restart:
	docker-compose restart app

stop:
	docker-compose stop
