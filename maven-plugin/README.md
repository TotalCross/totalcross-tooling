<!--
Copyright (C) 2020-2021 TotalCross Global Mobile Platform Ltda.
SPDX-License-Identifier: Apache-2.0
-->

# TotalCross Maven Plugin

This plugin packages TotalCross applications without a separate SDK download or
installation. Add the `totalcross-sdk` Java API dependency and this plugin
selects the required TotalCross SDK.

Original project creator: Italo Yeltsin. Fabio Sobral
([@flsobral](https://github.com/flsobral)) is the sole current maintainer.
Historical contributors are listed in [AUTHORS.md](AUTHORS.md); Git history is
the authoritative complete record. The project now lives in
`totalcross-tooling/maven-plugin`.

## Tasks
| Task                   | Description                                                                                  |
|------------------------|----------------------------------------------------------------------------------------------|
| totalcross:retrolambda | Uses retrolambda to make project new byte code versions, i.e., 1.8 compatible with java 1.6. |
| totalcross:package     | Executes the package process required to make totalcross applications.                       |
|                        |                                                                                              |

## Development

Run the Maven checks from this directory:

    mvn --batch-mode --no-transfer-progress test
    mvn --batch-mode --no-transfer-progress package

The source repository did not contain an authoritative license text. Apache-2.0
was assigned at the monorepo migration baseline; see [NOTICE](NOTICE).
