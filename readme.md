## Running project locally

- run `make repl` to start the repl 
- eval `dev/repl.clj` to spin up shadow cljs embedded server
- eval `src/clj/patients/core.clj` to start jetty web server

### Neovim & Conjure:

We will need two sessions: one for Clojure and other for ClojureScript
- run `:ConjureSessionClone` to clone current session
- run in neovim `:ConjureShadowSelect app` to connect to sdadow-cljs repl from Conjure
- to switch between session, run `:ConjureSessionSelect`, it will bring up the list and prompt you for session number

### Environment

Environment variables should be declared in `.env` file.
Needed variables are listed in `.env.example`.

## Migrations

By some reason, I could not get migrations to work with `ragtime` and CLJ tools, however it works with Leiningen: `ragtime` does not see migration files in `resources/migrations` directory.

## Tests

- `make run_test` to run tests in watch mode

Test configurations are stored in `tests.edn`.

Fixtures are set up using `setup` and `clean-up` hooks 

