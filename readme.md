## Running project locally

- run `make repl` to start project 
- eval `src/patients/core.clj` to start server

### Optionally:
- run `make server` to start clojure repl
- run `make front` to start shadow-cljs & browser repl
- run in neovim `:ConjureShadowSelect app` to connect to sdadow-cljs repl from Conjure

### Environment

Environment variables should be declared in `.env` file.
Needed variables are listed in `.env.example`.

## Running tests

`make run_test`

## Migrations

By some reason, I could not get migrations to work with `ragtime` and CLJ tools, however it works with Leiningen: `ragtime` does not see migration files in `resources/migrations` directory.

## Tests

Fixtures are set up using `setup` and `clean-up` hooks 



## TODO

- [ ] add migration mechanism
