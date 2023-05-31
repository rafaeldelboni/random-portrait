# random-portrait
Get a random portrait photo from Unsplash every X seconds.

_Uses an unofficial Unsplash API via `corsproxy.io` because it's a simple SPA without backend to handle my API keys._

## Todos
 - [x] Show current author and source links
 - [x] Refactor into smaller components
 - [x] Refactor into more pure functions
 - [ ] Write unit tests
 - [ ] Better state handling
 - [ ] Show formated list of past images with links
 - [ ] Don't let monitor sleep toogle 

## Commands

### Watch Dev/Tests
Build the developer build and start shadow-cljs watching and serving main in [`localhost:8000`](http://localhost:8000) and tests in [`localhost:8100`](http://localhost:8100)
```bash
npm run watch
```

### CI Tests
Run **Karma** tests targeted for running CI tests with *Headless Chromium Driver*
```bash
npm run ci:tests
```

### Release
Build the release package to production deploy
```bash
npm run release
```
## License

Copyright © 2023 Rafael Delboni

This is free and unencumbered software released into the public domain. For more information, please refer to http://unlicense.org
