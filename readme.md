## Running project locally

- run `make repl` to start the repl project 
- eval `src/clj/patients/core.clj` to start server
- run `make shadow` to start the UI server 

### Optionally:
- run in neovim `:ConjureShadowSelect app` to connect to sdadow-cljs repl from Conjure

### Environment

Environment variables should be declared in `.env` file.
Needed variables are listed in `.env.example`.

## Migrations

By some reason, I could not get migrations to work with `ragtime` and CLJ tools, however it works with Leiningen: `ragtime` does not see migration files in `resources/migrations` directory.

## Tests

- `make run_test` to run tests in watch mode

Test configurations are stored in `tests.edn`.

Fixtures are set up using `setup` and `clean-up` hooks 

