<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Unified tooling and preview evidence — Plan 08C

This continuation file records Plan 08C evidence after the historical journal
reached the text-file size limit. Full command output remains outside the repo.

- 2026-07-29: Plan 08C added an immutable version-1 JDK catalog with bundled,
  file, and in-memory test sources. It records concrete Temurin 17.0.16+8
  archives and SHA-256 values for macOS ARM64/x64, Linux x64, and Windows x64.
  The new catalog installer uses the shared atomic store, rejects traversal and
  incomplete layouts, probes the declared JAVA_HOME before completion, records
  catalog identity, and reuses completed installations offline. The focused
  tooling-core suite passed. On this macOS ARM64 host, the 176 MB archive was
  downloaded from the concrete release URL, matched
  `f9845abc8403f1d489402201064e7b9f2c57605d8717b85a95a15d94f882eeb7`,
  contained the declared `Contents/Home/bin/java` and `javac`, installed into a
  clean temporary store, passed capability probes, and then reused offline.
- 2026-07-29: SDKs before 7.3 keep the shared JDK-11 deploy and Retrolambda
  policy, so the catalog also gained Temurin 11.0.28+6 entries for the same
  macOS ARM64/x64, Linux x64, and Windows x64 matrix. On macOS ARM64, the
  176 MB archive matched
  `b5b46eb84aa2f301e739178aef0209c6843d6ad45b33f19dd39df4decdd29e9e`,
  contained its declared `Contents/Home/bin/java` and `javac`, passed the
  catalog installer probes in a clean store, and reused without network.
- 2026-07-29: the shared catalog resolver now probes an explicit `jdkPath`
  before consulting the catalog, selects only entries matching the requested
  Java major and normalized host, and reports an actionable `jdkPath` fallback
  when no offline or catalog installation can be used. Focused tooling-core
  tests passed; Gradle, Maven, CLI, and companion migration remains the next
  slice.
- 2026-07-29: Gradle package and Maven package/Retrolambda now obtain their
  tooling JDK from the catalog resolver, preserving a probed configured
  `jdkPath` when supplied. Maven `JavaJDKManager` and its Zulu-only dynamic
  download test were removed; no production Maven source references the legacy
  manager. Tooling publication to Maven Local, the tooling Java suite, Gradle
  plugin tests, and Maven plugin tests passed.
- 2026-07-29: the standalone CLI now resolves the Java 17 worker home through
  the shared catalog resolver and accepts a probed `--jdk-path` override. Its
  worker command is constructed from that selected home rather than the CLI
  process JVM. The CLI test and fat-JAR build passed.
- 2026-07-29: Gradle `totalcrossPreview` and Maven `totalcross:preview` now
  resolve Java 17 through the same catalog resolver before starting the CLI.
  Gradle wires `totalcross.jdkPath` into its preview task; Maven exposes the
  equivalent `totalcross.jdkPath` parameter. Both commands start the CLI from
  the selected JDK and pass `--jdk-path` onward to its worker. Gradle tests
  (including seven functional tests) and Maven tests passed with no failures.
