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
- 2026-07-29: both pre-IR plugins now state Java 17 as their loading runtime.
  Gradle rejects an older runtime with an actionable diagnostic, while Maven's
  generated plugin descriptor records `requiredJavaVersion` 17. README guidance
  separates this from the application target and catalog-selected deploy JDK.
  Gradle tests and Maven package generation passed.
- 2026-07-29: `ProjectModelCodec` now serializes and parses schema version 1,
  roots and outputs, dependencies, main class, SDK and JDK identities, Java and
  Retrolambda policy, arguments, platforms, and preview session. Gradle emits
  this model from `totalcrossProjectModel`; Maven emits it beside the preview
  session descriptor. The shared core, Gradle plugin, and Maven plugin tests
  passed. CLI and VS Code consumption remain open before the model gate closes.
- 2026-07-29: Gradle and Maven now start the CLI with `--model` rather than
  passing independently discovered project, class, and classpath values. The
  CLI reads the versioned model to obtain those values, so VS Code reaches the
  same path through its Gradle or Maven preview command. Tooling-core and CLI,
  Gradle plugin, and Maven plugin tests passed.
- 2026-07-29: removed the unused public Gradle `totalcrossTypedPackage` proof
  task and its implementation. `totalcrossPackage` remains the sole public
  Gradle packaging path, matching Maven's `totalcross:package`; the Gradle
  functional suite verifies that the removed task is no longer listed.
- 2026-07-29: `preview` and `run` now share process promotion but use distinct
  presentation modes. The CLI opens `AwtPreviewWindow` only for `run`; Gradle
  and Maven select the corresponding CLI command for their run tasks/goals.
  The duplicate Gradle run wrapper was removed. Tooling CLI, Gradle plugin, and
  Maven plugin tests passed.
- 2026-07-29: VS Code reload now compiles before it requests `reload` from the
  active coordinator, preserving the current session and frame when compilation
  fails. TypeScript compilation passed. The installed-extension test runner is
  currently blocked locally because its downloaded VS Code test bundle lacks the
  expected macOS `Contents/MacOS/Electron` executable; no behavior result is
  recorded from that E2E attempt.
- 2026-07-29: VS Code preview, build, and stop commands now prefer `mvnw` or
  `mvnw.cmd` when the Maven Wrapper exists, falling back to the system Maven
  executable only when it does not. The focused TypeScript compile passed.
- 2026-07-29: the preview Webview converts pointer input from displayed to
  intrinsic frame pixels. It sends resize requests with configured device width,
  height, density, and orientation (applied by ordering the dimensions), and
  observes Webview layout changes to reapply the device profile. TypeScript
  compilation passed.
- 2026-07-29: deterministic local VSIX construction now assembles only the
  compiled extension, resources, declared runtime dependency closure, and VSIX
  metadata. It normalizes timestamps and orders archive paths; two consecutive
  builds produced the same SHA-256. `npm run verify:vsix` verifies the local
  VSIX contains the extension entry point and declared runtime dependencies
  while excluding source, test output, and the VS Code test cache. The focused
  generator, Maven-model, layout, and migration-classifier suites passed (14
  cases). Generated and migrated projects use released Gradle plugin version
  0.1.0, do not add `mavenLocal()`, and no longer instruct users to run
  `publishToMavenLocal`. Runtime dependency audit passed with zero findings;
  four pre-existing high findings remain only in the development Mocha chain,
  whose automated fix requires a breaking upgrade.
- 2026-07-29: the VS Code test runner now accepts both the legacy macOS
  `Electron` executable name and the current `Code` name. It can install the
  generated VSIX into a temporary extension directory and run the same host
  suite from that installed `totalcross.vscode-totalcross-0.1.0` directory.
  Focused assertions prove reload emits success only after its build and sends
  no candidate-reload control after build failure. Both the development and
  installed-VSIX extension-host runs passed all 33 tests on VS Code 1.131.0.
- 2026-07-30: Gradle and Maven plugin documentation now identifies the
  versioned ProjectModel and frame/control protocol as the stable external
  integration boundary. SDK `totalcross.preview.*` APIs and the legacy
  live-preview server are documented as internal compatibility surfaces only;
  the Gradle description now also distinguishes streamed preview from native
  `run` presentation.
