<!--
Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Unified tooling and preview evidence

This append-only file records concise validation results for the coordinated
program. Full command output belongs in `/tmp` or build artifacts.

## Plan 01 bootstrap

- 2026-07-26: all eleven numbered plan files were present and non-empty in the
  starting folder.
- 2026-07-26: TotalCross cloned at `21a3d17e8cde3d3d2c45afc527448ffbee22e792`
  on `feature/392-feature-request-live-ui-preview-for-ides`; tooling cloned at
  `caaa01b0b1d26c1112491d298f5b29a81be8d313` and branched as
  `feature/unify-tooling-and-preview`.
- 2026-07-26: TotalCross `origin/master` is the remote default at
  `b7c25d7762aa326bf0c3a9bd384c173efad006da`, but the reviewed integration base
  is branch 392 at `21a3d17e8cde3d3d2c45afc527448ffbee22e792`.
- 2026-07-26: IR branch `ff81ab91a3ca08045198855ddb26874bd20e7b9a` is not an
  ancestor of the branch-392 integration base. Full baseline output is in
  `/tmp/totalcross-bootstrap-baseline.log`.
- 2026-07-26: file-size-policy unit tests passed 5/5; staged `git diff --check`
  and checker validation passed before commits `3487465` and `c27a313`.
- 2026-07-26: local TotalCross branch 392 was rebased onto `origin/master`
  `b7c25d7762aa326bf0c3a9bd384c173efad006da` without conflicts. Resulting local
  HEAD is `d20214f87d8f936d851f3d77b37603625b838b99`; `git diff --check` passed,
  and no push was performed.
- 2026-07-26: Plan 02 inventory identified duplicated Gradle/Maven SDK and JDK
  resolution in the paths recorded in the plan's `Surprises & Discoveries`.
- 2026-07-26: `./tooling-java/gradlew -p tooling-java :tooling-core:test
  --console=plain` passed 8 tests; publication to Maven Local also passed.
  Implementation checkpoint: `e8488ef`.
- 2026-07-26: TotalCross `artifactContentTest` passed after confirming the
  versioned narrow JAR names; tooling core and Gradle plugin tests passed.
  Paired commits: TotalCross `0716e10af`, tooling `d2b646f`.
- 2026-07-26: Plan 04 focused launcher/parser/runtime/preview validation passed
  5 tests, including copied-frame ownership and neutral command lifecycle
  forwarding. Full output is in `/tmp/totalcross-plan04-preview-test.log` and
  `totalcross/TotalCrossSDK/agent-logs/20260726-190501-test-full.log`.
- 2026-07-26: Plan 05 tooling Java tests passed across protocol, host/worker,
  and existing tooling-core modules. The fat CLI JAR and install distribution
  also built; full outputs are `/tmp/tooling-plan05-test.log` and
  `/tmp/tooling-plan05-package.log`.
- 2026-07-26: Plan 06 shared model tests and Gradle plugin functional tests
  passed; Maven plugin packaging and the focused SDK manager test passed. The
  legacy Maven JDK download test remains cache/layout dependent. Logs are
  `/tmp/tooling-plan06-core-test.log`, `/tmp/gradle-plugin-plan06-test.log`,
  and `/tmp/maven-plugin-plan06-package.log`.
- 2026-07-26: Plan 07 VS Code compilation and integration tests passed 21 tests,
  including the preview command selection test and all existing wizard and
  migration coverage. Full output is `/tmp/vscode-plan07-test.log`.
- 2026-07-26: Plan 08 reload/session checkpoint passed all tooling Java tests,
  including twenty successful candidate reloads and one failed candidate that
  did not replace the active preview. Android local-download migration remains
  open pending typed deploy integration.
- 2026-07-28: Plan 08 concrete external-tool validation passed. Official Protobuf
  21.0 assets were hashed for Linux x86_64/ARM64, macOS x86_64/ARM64, and
  Windows x86_64/ARM64 reuse; Bundletool 1.15.6 was hashed from the complete
  29,105,379-byte JAR. The probes returned `libprotoc 3.21.0` and `1.15.6`.
- 2026-07-28: `./gradlew :tooling-core:test --console=plain` passed 13 tests in
  `/tmp/tooling-plan08-core-test-final.log`, covering catalog metadata,
  checksum rejection, version-probe rejection, atomic installation, offline
  reuse, and concurrent installation.
- 2026-07-28: `./gradlew test --console=plain` passed in the Gradle plugin;
  evidence is `/tmp/gradle-plugin-plan08-test-final.log`.
- 2026-07-28: `./gradlew compileJava --console=plain` passed in
  `totalcross/TotalCrossSDK`; evidence is
  `/tmp/totalcross-plan08-sdk-compile-final.log`. The artifact-boundary test
  was not runnable without its required `-Dtotalcross.artifact.dir` property.
- 2026-07-28: license/provenance validation passed and its 19 unittest cases
  passed; evidence is `/tmp/tooling-plan08-license.log`.

## Plan 08B stabilization slice

- 2026-07-28: tooling-java full tests passed 14 tests after canonical host/worker
  handshake, reflective reload/input hooks, CLI `stop`, and staged publication;
  final output is `/tmp/tooling-plan08b-canonical-test.log` (subsequent focused
  reruns also passed).
- 2026-07-28: Gradle plugin functional suite passed 21 tests after preview
  classpath forking and moving `totalcrossPackage` behind `DeployService`;
  final output is `/tmp/gradle-plan08b-final-test.log`.
- 2026-07-28: Maven focused tests and `-DskipTests package` passed after preview,
  preview-stop, subprocess Java-17 tooling, and shared deploy integration;
  full Maven tests remain unavailable because concurrent legacy cache tests
  started large network downloads.
- 2026-07-28: live-preview-server tests passed 3 tests with SDK 7.2.2 using
  `-PtotalcrossSdkVersion=7.2.2`; the HTTP server is documented as legacy.
- 2026-07-28: VS Code compile and integration suite passed 30 tests. The public
  command set is now `Preview`, `Run`, `Stop Preview`, and `Preview Diagnostics`;
  old HTTP/Webview commands remain testable but are no longer activated or
  contributed publicly.
- 2026-07-28: SDK `compileJava` passed after adding public preview resize,
  pointer, and key injection hooks; final output is
  `/tmp/totalcross-plan08b-sdk-compile.log`.
- 2026-07-28: aggregate `totalcross-sdk` and five narrow SDK Maven publications
  were written to `totalcross/TotalCrossSDK/build/repo`; tooling protocol,
  host, worker, core, and CLI publications were written to
  `/tmp/totalcross-plan08b-staging`.
- 2026-07-28: direct CLI fixture probe correctly emitted `started` but did not
  emit a first frame with the empty `PreviewMainWindow.initUI` fixture on this
  macOS host; the release-level first-frame claim remains open.
- 2026-07-29: the SDK gained an optional `aggregateCompatibilityCheck` japicmp
  task. Comparing the current aggregate JAR with a clean JAR built from the
  previous SDK checkpoint `a020512e4` passed with no binary-incompatible
  changes; the report is `totalcross/TotalCrossSDK/build/reports/aggregate-compatibility.txt`.
  A strict comparison against the older cached 7.2.0 artifact still reports
  historical Launcher/deployer incompatibilities predating this checkpoint.
- 2026-07-29: Maven dependency resolution from a fresh local repository, with
  only the staged SDK repository plus Maven Central and no `mavenLocal`, first
  exposed the missing `com.totalcross.annotations:totalcross-annotations:1.0.0`
  staging dependency. After publishing that existing release artifact into
  the staging repository, aggregate and preview-runtime SDK artifacts resolved;
  tooling CLI resolution from `/tmp/totalcross-plan08b-staging` also resolved
  all internal host/worker/protocol dependencies. The clean repository was
  `/tmp/totalcross-clean-m2.Symu0K`.
- 2026-07-29: real temporary Gradle and Maven projects compiled the fixture,
  started the shared coordinator, received a non-empty `320x568` PNG, and
  stopped through their build-tool goals. Gradle logs are
  `/tmp/totalcross-gradle-e2e-preview4.log` and
  `/tmp/totalcross-gradle-e2e-stop4.log`; Maven produced
  `/tmp/totalcross-maven-e2e.i20RTr/target/totalcross/preview-frame.png` and
  its stop goal removed the session descriptor.
- 2026-07-29: Gradle and Maven preview tasks now delete stale frames, wait for
  the first non-empty frame before reporting success or recording the PID, and
  persist coordinator output in `preview.log`; a missing frame fails clearly
  with the log path. The temporary e2e run also exposed stale SNAPSHOT metadata,
  which was corrected by republishing the current tooling chain to staging.
- 2026-07-29: SDK/JDK/Java/Retrolambda compatibility policy was centralized in
  `tooling-core`. Gradle package validation and Maven package/Retrolambda paths
  now consume the same SDK-generation rules; Maven selects JDK 11 for SDKs
  before 7.3 and JDK 17 for newer SDKs. Core policy tests, Gradle plugin tests,
  Maven compilation/package, targeted Retrolambda coverage, and JDK-generation
  selection coverage passed.
- 2026-07-29: a local VSIX was packaged as
  `vscode-extension/totalcross-preview-local.vsix` and installed successfully
  in the real VS Code as `totalcross.vscode-totalcross@0.1.0`. The VS Code
  integration suite then passed all 30 tests; those tests still load the
  development extension path, so installed-VSIX activation and the full
  installed-project flow remain a separate release gate.
- 2026-07-29: the isolated Gradle matrix used staging repositories and an
  isolated Gradle home. It passed first frame, valid source compile, failed
  compile with the old coordinator still alive, repaired compile, reload,
  resource processing, resize, pointer, key, and stop with no remaining
  coordinator. Logs are `/tmp/totalcross-gradle-reload-matrix-start.log`,
  `/tmp/totalcross-gradle-reload-matrix-sdkfix-failed.log`,
  `/tmp/totalcross-gradle-reload-matrix-sdkfix-repaired.log`, and
  `/tmp/totalcross-gradle-reload-matrix-stop.log`.
- 2026-07-29: the equivalent Maven matrix used the clean repository
  `/tmp/totalcross-maven-home4.cb3WCM`. It passed first frame, failed compile
  while preserving the detached coordinator, repaired compile, reload, resource
  processing, resize, pointer, key, and `totalcross:preview-stop`; the descriptor
  was removed and the coordinator exited. Evidence is in
  `/tmp/totalcross-maven-reload-matrix-detached3-start.log`,
  `/tmp/totalcross-maven-reload-matrix-detached3-failed.log`,
  `/tmp/totalcross-maven-reload-matrix-detached3-repaired.log`, and
  `/tmp/totalcross-maven-reload-matrix-detached3-stop.log`.
- 2026-07-29: SDK resize commands now run on the preview event thread to avoid
  inconsistent frame payloads during concurrent rendering; Maven preview uses a
  detached POSIX session on macOS/Linux-compatible hosts so a failed Maven
  compile does not terminate the active coordinator. Fixes are TotalCross
  `405275156` and tooling `0a0e04c`.
- 2026-07-29: a published Gradle plugin was resolved from the isolated staging
  repository without `mavenLocal` and reached `totalcrossPackage`; the smoke
  was stopped because the synthetic SDK home contained only the deploy JAR and
  lacked the complete distribution layout required by real `tc.Deploy`. The
  shared typed `DeployService` contract remains covered by the Gradle functional
  suite; release-grade real packaging remains open.
- 2026-07-29: the official `TotalCross-7.2.2.zip` was extracted to a temporary
  SDK home with `dist/vm`, `dist/libs`, and the deployer layout. The real
  packaging smoke exposed a non-daemon telemetry thread and SDK-home discovery
  gap in the legacy adapter; tooling commit `e8c79a4` now runs the invocation in
  a daemon thread and applies the requested SDK home as temporary `user.dir`.
  Tooling-java (14 tests), the Gradle plugin suite, and Maven packaging passed;
  the real smoke still ended with `Connection reset` after a long external
  operation, so release-grade typed packaging remains open. Log:
  `/tmp/totalcross-gradle-typed-deploy-realsdk-fixed.log`.
- 2026-07-29: tooling commit `d0c6e1e` preserves captured legacy deploy output
  when the reflective invocation fails, making future packaging failures
  diagnosable without changing the typed result contract. The tooling-core
  deploy test passed in `/tmp/tooling-core-deploy-diagnostics.log`.
- 2026-07-29: preview version gating was verified with the cached public SDK
  7.2.0. A minimal application compiled against that aggregate JAR caused the
  CLI to emit a structured `error` requiring
  `LauncherRuntime.startPreviewFrames`/`PreviewFrameConsumer`, return exit 1,
  and leave no coordinator process. Log:
  `/tmp/totalcross-preview-old-sdk-gating.log`.
- 2026-07-29: `aggregateCompatibilityCheck` against the cached public
  `totalcross-sdk-7.2.0.jar` completed with strict mode disabled and reported
  real binary incompatibilities in `totalcross.Launcher`, converter/deployer
  classes, and removed public nested classes. The report is
  `totalcross/TotalCrossSDK/build/reports/aggregate-compatibility.txt`; this is
  a release-baseline decision, not a preview-gating failure.
- 2026-07-29: the installed VSIX was loaded by the local VS Code executable
  (without downloading a test Electron) and passed all 30 integration tests,
  including command activation, preview client, project generation, migration,
  and rollback behavior. Output is `/tmp/vscode-installed-vsix-local-test.log`;
  the manual installed-project E2E remains open.
- 2026-07-29: a manual installed-project attempt opened the VSIX host against
  `/tmp/totalcross-vscode-installed-e2e.Wzu5Er`. The workspace opened in
  Restricted Mode and `TotalCross: Preview` was absent from the command
  palette; a Java language-server warning then made the window unavailable to
  accessibility control. No preview frame or stop result was recorded, so the
  installed-project E2E gate remains open.
- 2026-07-29: a fresh Gradle real-packaging smoke used the official SDK 7.2.2
  distribution, the current local plugin, Java target 8, and test-only local
  plugin resolution. It passed `clean totalcrossPackage` and generated
  `build/totalcross/install/linux/MainWindow`, `MainWindow.tcz`, the bundled
  runtime files, and `libtcvm.so`. Output is
  `/tmp/totalcross-gradle-typed-deploy-real-success.log`. The plugin fix in
  `TotalCrossPlugin.java` pins dependency variant selection to the Java-17
  compiler JVM while preserving the independent SDK bytecode policy. The task
  succeeds, although the legacy SDK's asynchronous telemetry emits a
  non-fatal `NoClassDefFoundError` after the deployer returns.
- 2026-07-29: the Maven real-package retry downloaded the 300,034,744-byte
  JDK 11 archive successfully after moving a corrupt 59 MB cache artifact to
  `/tmp/totalcross-corrupt-zulu_jdk_11.zip`. The Maven mojo now selects
  `dist/totalcross-sdk.jar` from the configured SDK installation, matching the
  Gradle deploy classpath; this fixed SDK-home discovery for package mode. The
  official SDK 7.2.2 fixture passed `totalcross:package` and generated
  `target/install/linux/PreviewMainWindow`, `PreviewMainWindow.tcz`, the
  `TCBase/TCFont/TCUI` and Material Icons packages, and `libtcvm.so`.
  Logs: `/tmp/totalcross-maven-real-package-final-success.log` and
  `/tmp/totalcross-maven-plugin-test-current.log`.
- 2026-07-29: network-dependent checks passed: Gradle
  `sdkSourceNetworkTest` passed in `/tmp/gradle-plugin-sdk-source-network.log`,
  and Maven `mvn test` passed 8 tests including the live JDK 11 download test.
  The legacy SDK still emits a non-fatal asynchronous telemetry
  `NoClassDefFoundError` after successful deploy; generated outputs and the
  Maven result are successful.
- 2026-07-29: the excluded SDK `AnonymousUserDataTest` was run explicitly with
  a temporary Gradle init script that removed only the test exclusion. All
  three network cases reached the configured Heroku endpoint but failed with
  HTTP 404 / `No such app`; direct `curl` confirmed the same response from
  `aqueous-plateau-93003.herokuapp.com`. This is an unavailable external test
  service, not a local network failure. Logs:
  `/tmp/totalcross-plan08b-anonymous-user-data-network-real.log` and
  `/tmp/totalcross-anonymous-user-data-endpoint.body`.
- 2026-07-29: the public aggregate compatibility check was rerun strictly
  against cached public `totalcross-sdk-7.2.0.jar` and failed as expected. The
  report still contains concrete public API removals in `totalcross.Launcher`,
  nested stream/font classes, `IllegalStateException4D`, and deploy/converter
  classes; the gate remains a release-owner compatibility/versioning decision.
  Log: `/tmp/totalcross-plan08b-aggregate-compatibility-rerun.log`.
- 2026-07-29: the SDK standard suite was rerun with its required
  `totalcross.artifact.dir` test property injected into the Gradle Test worker;
  it passed, and the dedicated `artifactContentTest` also passed. The first
  plain `test` invocation's three failures were environment setup errors from
  the missing property, not SDK assertions. Logs:
  `/tmp/totalcross-plan08b-sdk-standard-test-fixed-env.log` and
  `/tmp/totalcross-plan08b-artifact-boundaries-rerun.log`.
- 2026-07-29: `TotalCrossSDK/build.gradle` now excludes the tagged
  `artifact-boundary` tests from the ordinary `test` task; the dedicated
  `artifactContentTest` remains responsible for them. After TotalCross commit
  `62a4df7b5`, both `./gradlew test` and `./gradlew artifactContentTest` pass
  without temporary test-property injection. Logs:
  `/tmp/totalcross-plan08b-sdk-standard-test-final.log` and
  `/tmp/totalcross-plan08b-artifact-boundaries-final.log`.
- 2026-07-29: the user accepted the strict aggregate comparison differences as
  a documented pre-IR waiver: internal converter/deployer utilities, the
  intentional `IllegalStateException4D` package relocation, and the historical
  `Launcher` superclass/nested-helper differences do not block this release;
  its construction and argument execution contract remains the accepted public
  surface. TotalCross commit `9a36178ef` also disables `AnonymousUserData` in
  launcher/deploy runtime paths and disables its endpoint-dependent test. SDK
  `test` plus `artifactContentTest` pass after this change in
  `/tmp/totalcross-plan08b-sdk-telemetry-disabled-validation-final.log`. The
  accepted compatibility report completes in non-strict mode at
  `/tmp/totalcross-plan08b-aggregate-compatibility-accepted.log`.
