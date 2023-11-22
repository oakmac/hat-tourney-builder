# Hat Tournament Builder

UI tool for sorting teams for an Ultimate hat tournament.

## TODO

- [ ] put player strength display on PlayerBox
- [ ] export player / team list as CSV
- [ ] styling
- [ ] sort order on teams should persist on page refresh
- [ ] Link groups should not have only one player
- [ ] search All Players list
- [ ] ability to edit team name
- [ ] ability to delete team column
- [x] ability to refresh the page and not lose data
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
