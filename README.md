[![Build Status](https://travis-ci.org/rahulsom/swaggydoc.svg?branch=develop)](https://travis-ci.org/rahulsom/swaggydoc)

![Unmaintained](https://img.shields.io/badge/status-unmaintained-yellow.svg) This repository is not actively maintained. If you are interested in taking it over, please let me know.

## Documentation

User Documentation is at https://rahulsom.github.io/swaggydoc

## Contributing

Before you can run any other commands, you will have to obtain the swagger assets.

```bash
./bowerize.sh
```

Running grails3 plugin in dev mode
```bash
./gradlew swaggydoc-grails3:bootRun
```

Running grails2 plugin in dev mode
```bash
./gradlew grails2:run
```
