## Running project locally

### Running ClojureScript

- run `make front` to start shadow-cljs & repl
- run in neovim `:ConjureShadowSelect app` to connect to sdadow-cljs repl from Conjure

### Environment

Environment variables should be declared in `.env` file.
Needed variables are listed in `.env.example`.

To run repl, use `make repl` command.

## Running tests

`make DB_NAME=<name> run_test`

## Migrations

By some reason, I could not get migrations to work with `ragtime` and CLJ tools, however it works with Leiningen: `ragtime` does not see migration files in `resources/migrations` directory.

## Tests

Fixtures are set up using `setup` and `clean-up` hooks 



## TODO

- [ ] add migration mechanism
