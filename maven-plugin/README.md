<!--
Copyright (C) 2020-2021 TotalCross Global Mobile Platform Ltda.
Copyright (C) 2022-2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->

> [!IMPORTANT]
> This project has moved to the
> [`TotalCross/totalcross-tooling`](https://github.com/TotalCross/totalcross-tooling)
> repository under [`maven-plugin/`](https://github.com/TotalCross/totalcross-tooling/tree/main/maven-plugin).
> Development continues there. This repository is retained for historical
> issues, pull requests, releases, and links and is intended to be archived.

# TotalCross Maven Plugin
This is the totalcross maven plugin. It helps building TotalCross applications without download or instaall anything else. You just need to have totalcross-sdk java api set in your dependencies and this plugin takes care of downloading the right TotalCross SDK.

The current preview implementation uses the Java-17 `tooling-java` host/worker
distribution. Maven and the build that invokes this plugin therefore require a
Java 17 runtime. `totalcross:preview`, `totalcross:run`, and
`totalcross:preview-stop` share the authenticated worker lifecycle; packaging
remains the compatibility deploy path. This loading requirement is independent
from the catalog-selected JDK used to package an application: SDK 7.3.0 and
newer use JDK 17, while earlier SDKs can use JDK 11. Configure
`totalcross.jdkPath` only to override or provide a compatible JDK when the
catalog does not support the host.

The stable integration boundary is the versioned project model and the
cross-process frame/control protocol. A preview candidate is promoted only
after readiness and its first valid frame, preserving the prior worker and
displayed frame when a build or candidate fails. The `totalcross:preview`
coordinator and worker JVMs run with `java.awt.headless=true` and do not create
a desktop Java window. `totalcross:run` uses that
same lifecycle with a native window. SDK `tc.preview.*` APIs and the
legacy preview server are internal compatibility surfaces, not public reload
or IDE integration APIs.

## Tasks
| Task                   | Description                                                                                  |
|------------------------|----------------------------------------------------------------------------------------------|
| totalcross:retrolambda | Uses retrolambda to make project new byte code versions, i.e., 1.8 compatible with java 1.6. |
| totalcross:package     | Executes the package process required to make totalcross applications.                       |
|                        |                                                                                              |
