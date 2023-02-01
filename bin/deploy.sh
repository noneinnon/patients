#!/bin/bash

echo "--- pulling new stuff"
git stash & git pull --rebase
echo "--- building & starting"
docker-compose down --remove-orphans && make start
echo "--- running migrations"
docker-compose exec app make apply-migrations

