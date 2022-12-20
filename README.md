# Hat Tournament Builder

UI tool for sorting teams for an Ultimate hat tournament.

## Notes

- https://github.com/eanway/SortingHat
- drag-and-drop names onto columns
- [MDN Drag and Drop](https://developer.mozilla.org/en-US/docs/Web/API/HTML_Drag_and_Drop_API)
- [MDN Drag and Drop Example1](https://mdn.github.io/dom-examples/drag-and-drop/copy-move-DataTransfer.html)
- link players together
- pink boxes for women, blue for guys
- variable number of columns
- average score per column, num of females, guys

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
