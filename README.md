# Less4clj
[![Clojars Project](https://img.shields.io/clojars/v/deraen/less4clj.svg)](https://clojars.org/deraen/less4clj)
[![Build Status](https://travis-ci.org/Deraen/less4clj.svg?branch=master)](https://travis-ci.org/Deraen/less4clj)
[![AppVeyor](https://img.shields.io/appveyor/ci/deraen/less4clj.svg?maxAge=2592000&label=windows)](https://ci.appveyor.com/project/Deraen/less4clj)
[![Downloads](https://jarkeeper.com/deraen/less4clj/downloads.svg)](https://jarkeeper.com/deraen/less4clj)
[![Dependencies Status](https://jarkeeper.com/deraen/less4clj/status.svg)](https://jarkeeper.com/deraen/less4clj)

Clojure wrapper for [Less4j](https://github.com/SomMeri/less4j) Java implementation of Less compiler.
This repository also contains [Boot](http://boot-clj.com/) and [Leiningen](http://leiningen.org/) tasks.

For parallel Sass library check [sass4clj](https://github.com/Deraen/sass4clj)

## Features

- Load imports directly from Java classpath (e.g. [Webjars](https://www.webjars.org/))
    - Add dependency `[org.webjars.bower/bootstrap "3.3.6"]` to use [Bootstrap](http://getbootstrap.com/)

## Boot [![Clojars Project](https://img.shields.io/clojars/v/deraen/boot-less.svg)](https://clojars.org/deraen/boot-less) [![Downloads](https://jarkeeper.com/deraen/boot-less/downloads.svg)](https://jarkeeper.com/deraen/boot-less)

* Provides the `less` task (`deraen.boot-less/less`)
* For each `.main.less` file in the fileset creates equivalent `.css` file.
* Check `boot less --help` for task options.

## Leiningen [![Clojars Project](https://img.shields.io/clojars/v/deraen/lein-less4clj.svg)](https://clojars.org/deraen/lein-less4clj) [![Downloads](https://jarkeeper.com/deraen/lein-less4clj/downloads.svg)](https://jarkeeper.com/deraen/lein-less4clj)

* Provides the `less4clj` task
* For each `.main.less` file in source-dirs creates equivalent `.css` file.
* Check `lein help less4clj` for options.

## Clj

Test in the repository:

`clj -m less4clj.main --source-paths test-resources`

Check `clj -m less4clj.main --help` for options.

## Import load order

Loading order for `@import "{name}";` on file at `{path}`

1. Local file at `{path}/{name}.less`
2. Classpath resource `(io/resource "{name}.less")`
3. Classpath resource `(io/resource "{path}/{name}.less")`
4. Webjar asset
    - Resource `META-INF/resources/webjars/{package}/{version}/{path}` can be referred using `{package}/{path}`
    - For example `@import "bootstrap/less/bootstrap.less";` will import  `META-INF/resources/webjars/bootstrap/3.3.6/less/bootstrap.less`

## FAQ

### Semantic-UI theme.config

Semantic-UI needs [theme.config](https://github.com/Semantic-Org/Semantic-UI/blob/master/src/theme.config.example)
file in your project. Add this file to your classpath under path `META-INF/resources/webjars/semantic-ui/2.2.10/src/theme.config`, e.g under `resources/` folder.

### Log configuration

If you don't have any slf4j implementations you will see a warning:

```
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
```

To disable this add a no operation logger to your project. As this is only required
on build phase, you can use `:scope "test"` so that the dependency is not
transitive and is not included in uberjar. Alternatively you can add this
dependency to your Leiningen dev profile.

```
[org.slf4j/slf4j-nop "1.7.13" :scope "test"]
```

## License

Copyright © 2014-2017 Juho Teperi

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
