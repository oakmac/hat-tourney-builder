# Hat Tournament Builder

UI tool for sorting teams for an Ultimate hat tournament.

## TODO

- multiple "projects" in the browser
- save state on every change; ability to refresh page
- search All Players list
- dynamic Teams Columns
- import player list
- export player / team list
- styling
- package Sortable with the CLJS?
- https://github.com/eanway/SortingHat

## Development

Make sure ClojureScript tooling is installed, then:

```sh
## one-time library install
bun install

## run a local webserver out of the public/ folder
bun run server

## build public/js/main.js
npm run build
```

## License

[ISC License](LICENSE.md)
