<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Implement the shared store and vendor-neutral JDK policy

This ExecPlan is Plan 02 of the unified tooling and preview program. Read the
canonical state file first and execute only after Plan 01 is complete.

## Purpose / Big Picture

Provide one tested Java library that every TotalCross tool can use to locate,
download, verify, install, and select SDKs, JDKs, and reusable external tools.
A developer can keep multiple versions side by side and receives a clear
diagnostic when a downloaded JDK cannot start required subprocesses.

## Working Set and Resume Protocol

Read:

    .agent/state/unified-tooling-preview.md
    .agent/plans/unified-tooling-preview/02-shared-store-and-jdk.md

Inspect only current SDK/JDK resolver code in the Gradle and Maven plugins after
the plan names it. Do not refactor either plugin yet. Record duplicated behavior
as migration inputs.

## Progress

- [x] Inventory current Gradle and Maven SDK/JDK resolution.
- [x] Create the `tooling-java` build and `tooling-core` module.
- [x] Implement native data/cache root resolution.
- [x] Implement locked, checksum-verified, atomic installations.
- [x] Implement concrete SDK and JDK metadata.
- [x] Implement vendor-neutral JDK candidates and explicit overrides.
- [x] Implement subprocess capability probes.
- [x] Add focused tests for all supported host platforms.
- [x] Publish the core artifact to Maven Local for later plans.
- [ ] Commit and update state to Plan 03.

## Current Architecture and Scope

Create a standalone Gradle build at:

    tooling-java/
      settings.gradle
      build.gradle
      gradle.properties
      tooling-core/

The module group is `com.totalcross.tooling`; use one repository-wide snapshot
version property. Target Java 17. Keep the core free of Gradle, Maven, VS Code,
AWT, and TotalCross SDK classes.

Use these packages:

    com.totalcross.tooling.platform
    com.totalcross.tooling.store
    com.totalcross.tooling.download
    com.totalcross.tooling.process
    com.totalcross.tooling.jdk
    com.totalcross.tooling.sdk

Data roots are:

    macOS: ~/Library/Application Support/TotalCross
    Windows: %LOCALAPPDATA%\TotalCross
    Linux: ${XDG_DATA_HOME:-~/.local/share}/TotalCross

Cache roots are:

    macOS: ~/Library/Caches/TotalCross
    Windows: %LOCALAPPDATA%\TotalCross\Cache
    Linux: ${XDG_CACHE_HOME:-~/.cache}/TotalCross

Install immutable concrete versions beneath `sdk/`, `jdk/`, `tooling/`, and
`tools/`. Downloads and extraction use cache staging. A completed installation
contains metadata, checksum, source URL, host platform, and an installation
completion marker.


## Cross-plan safety and size policy

Run only one plan in this set at a time. Preserve unrelated local work. Never use
`git reset --hard`, `git clean -fd`, force-push, history rewriting, tag deletion,
or repository archival unless the user explicitly requests that exact operation.

Every created or modified text file must remain at or below 20 KiB and at or
below approximately 600 lines. Check the staged diff before every commit with
the policy script created by Plan 01. If an existing non-protected file already
exceeds either limit, split it by responsibility before making the functional
change. Do not split a protected IR-related file merely to satisfy this rule.
The protected paths are:

    TotalCrossSDK/src/main/java/tc/tools/converter/**
    TotalCrossSDK/src/test/java/tc/tools/converter/**
    TotalCrossVM/src/tcvm/ir/**
    TotalCrossVM/src/tcvm/jit/**
    TotalCrossVM/src/tcvm/aot/**
    TotalCrossVM/src/tests/ir/**
    docs/architecture/bytecode/**

The exception follows those logical files during the first history-preserving
move after the IR merge. Do not refactor them for size as part of this program.
Generated files, third-party code, caches, and build output must not be committed.

Use token-efficient execution. Read the active state file first, inspect only
the named paths for the active slice, run focused validation, store full verbose
output in `/tmp` or build artifacts, and record only concise results and paths.
Do not repeatedly dump large plans, logs, diffs, or generated files.

## Plan of Work

First inventory only the resolver and compatibility classes in the current
Gradle and Maven plugins. Record paths and behavior in state; do not copy their
implementations blindly.

Create small immutable request/result types. At minimum define:

    HostPlatform
    StoreLayout
    ArtifactCoordinate
    DownloadRequest
    InstallRequest
    InstalledArtifact
    FileLockManager
    ChecksumVerifier
    ArchiveExtractor
    ProcessRequest
    ProcessResult
    JdkRequest
    JdkCandidate
    JdkInstallation
    JdkCapabilityProbe
    SdkRequest
    SdkInstallation

`HostPlatform` normalizes OS and architecture without treating x86 as a default.
`StoreLayout` accepts explicit data/cache overrides for tests and advanced users.
All installation code writes to a unique staging directory, verifies the
archive, extracts safely against path traversal, writes metadata, then atomically
renames into the final immutable directory. Concurrent installers use a
per-coordinate lock and recheck completion after acquiring it.

JDK selection accepts an explicit path first. Otherwise it evaluates ordered
vendor candidates. Implement Corretto, Temurin, and regular non-CRaC Zulu as
providers behind a common interface. Do not call a `latest` endpoint without
persisting the returned concrete version and build. Do not accept a candidate
only because `bin/java` exists.

The capability probe runs with timeouts and verifies:

    bin/java -version
    bin/javac -version
    a Java ProcessBuilder child with stdout and stderr
    process termination and timeout cleanup

On macOS it also attempts `/usr/bin/xattr` through ProcessBuilder. An absent
quarantine attribute is not a JDK failure; inability to create or wait for the
process is. When a known `protoc` executable is supplied, the probe runs
`protoc --version`. Keep this argument optional so JDK selection can be tested
before Plan 08 installs global tools.

Add structured diagnostics that identify vendor, concrete version, failed
capability, exit code, and concise error. Never include credentials or signed
URLs.

Tests use temporary roots and local HTTP fixtures or stub download providers.
Cover macOS ARM64 layout, Windows layout, Linux XDG overrides, archive traversal,
checksum mismatch, interrupted staging, lock contention, explicit JDK path,
candidate fallback, CRaC rejection, process timeout, and xattr exit semantics.

Publish `tooling-core` to Maven Local with:

    ./tooling-java/gradlew -p tooling-java       :tooling-core:publishToMavenLocal --console=plain

## Surprises & Discoveries

- Observation: the exact current Zulu failure was associated with a CRaC build,
  while a user-provided JDK path was used as a successful comparison.
  Evidence: confirm current resolver filters and tests during inventory.

- Observation: Gradle and Maven duplicate download, extraction, SDK/JDK cache,
  and platform normalization logic. Gradle's `JdkResolver` validates only an
  executable `bin/java` and requests a Zulu `latest` URL; Maven's
  `JavaJDKManager` fixes Zulu JDK 11 and uses the app-data directory without a
  checksum or completion marker.
  Evidence: `gradle-plugin/src/main/java/com/totalcross/gradle/{SdkResolver,JdkResolver,ArchiveDownloader,CacheLock}.java`
  and `maven-plugin/src/main/java/com/totalcross/{DownloadManager,JavaJDKManager,TotalCrossSDKManager}.java`.

- Observation: the live-preview branch was rebased locally onto
  `origin/master` before Plan 02 implementation, as explicitly requested.
  Evidence: local TotalCross HEAD `d20214f87d8f936d851f3d77b37603625b838b99`,
  `origin/master` is an ancestor, and the rebase had no conflicts; no push was
  performed.

- Observation: the first timeout implementation attempted to read a process
  stream after forcible termination and received `java.io.IOException: Stream
  closed`.
  Evidence: `/tmp/tooling-core-test.log` captured the failure; `ProcessRunner`
  now treats closed timeout streams as empty and the full suite passes 8 tests.

## Decision Log

- Decision: select JDKs by verified capability, not vendor name.
  Rationale: vendor and archive labels do not prove ProcessBuilder behavior.
  Date/Author: 2026-07-26 / OpenAI.

- Decision: use immutable concrete-version installations with atomic promotion.
  Rationale: concurrent plugins and interrupted downloads must not corrupt shared
  state.
  Date/Author: 2026-07-26 / OpenAI.

## Validation and Acceptance

Run `:tooling-core:test` first. Run publication only after focused tests pass.
Save verbose Gradle output in `/tmp/tooling-core-test.log` and report a concise
tail on failure.

Acceptance is demonstrated by a test or small CLI fixture that installs two JDK
versions side by side, rejects a simulated broken candidate, selects the next
candidate, and leaves a complete metadata record. The staged size checker and
`git diff --check` must pass.

## Risks and Open Questions

Vendor APIs may require separate metadata adapters. Keep provider-specific JSON
outside common request types. Windows archive symlink behavior may differ; tests
must verify the selected extraction library on each available host and record
unavailable hosts for CI.

## Idempotence and Recovery

A repeated install returns the existing completed installation after checksum
and metadata verification. An incomplete staging directory is safe to remove
only when its lock is held and it is older than the active process. Never delete
an installation merely because a provider no longer lists it.

## Outcomes & Retrospective

The shared Java core now provides platform roots, concrete artifact identity,
checksum verification, safe ZIP extraction, lock-coordinated atomic installs,
metadata/completion markers, process execution with timeout cleanup, JDK
capability probing, vendor candidates, selection diagnostics, and SDK install
adapters. It is independent of Gradle, Maven, AWT, and TotalCross SDK classes.
The plugin resolvers were intentionally left unchanged; later plans will migrate
consumers after the boundary is proven.

Eight focused tests pass on the current macOS ARM64 host, including simulated
Windows/Linux layouts, ZIP traversal rejection, checksum mismatch, side-by-side
coordinates, lock contention, process streams/timeouts, JDK fallback/CRaC
rejection, and provider URL checks. The artifact was published to Maven Local.

## Revision Note

2026-07-26: separated store/JDK work into a bounded plan and required concrete
metadata plus real subprocess capability probes.

2026-07-26: implemented the standalone Java 17 core, added focused tests, fixed
timeout-stream cleanup after the first test failure, and published
`com.totalcross.tooling:tooling-core:0.1.0-SNAPSHOT` to Maven Local.

## Editorial Report

This section is mandatory at completion. Keep it factual and evidence-based.

### Editorial Summary

Gradle and Maven previously carried separate SDK/JDK download and cache logic,
including a Zulu `latest` endpoint and weak executable-only validation. Plan 02
created a vendor-neutral Java 17 core that stores concrete immutable
installations, verifies archives, and proves external process capability before
selection.

### Original Plan versus Actual Outcome

The standalone build, store, process, JDK, SDK types, focused tests, and Maven
Local publication were completed. The existing Gradle and Maven plugins were
not migrated, as required by the plan's bounded scope; they remain migration
inputs for later plans.

### What Changed

`tooling-java/tooling-core` contains platform/store/download/process/jdk/sdk
packages. `StoreLayout` handles macOS, Windows, and Linux roots;
`ArtifactInstaller` creates metadata and a completion marker after atomic
promotion; `JdkCapabilityProbe` and `JdkSelector` validate candidates and emit
diagnostics. The `tooling-java` Gradle wrapper and Maven publication are now
available for later consumers.

### Decisions and Trade-offs

The core uses immutable coordinate directories and per-coordinate lock files,
which favors reproducibility over automatic replacement. Provider adapters build
concrete-version URLs and reject CRaC Zulu candidates; provider API discovery and
download metadata remain outside this slice. Explicit JDK paths take precedence
over ordered candidates.

### Unexpected Problems and Discoveries

Timeout cleanup exposed a closed-stream race after process termination. The
runner now returns a timed-out result without failing the caller when a child
closes its pipes during cleanup.

### Validation and Measurable Results

`./tooling-java/gradlew -p tooling-java :tooling-core:test --console=plain`
passed 8 tests. `./tooling-java/gradlew -p tooling-java
:tooling-core:publishToMavenLocal --console=plain` passed and installed the
snapshot artifact under the local Maven repository. Full logs are in
`/tmp/tooling-core-test.log` and `/tmp/tooling-core-publish.log`.

### Useful Evidence and Examples

The focused test class is
`tooling-java/tooling-core/src/test/java/com/totalcross/tooling/ToolingCoreTest.java`.
The inventory inputs are the existing Gradle `SdkResolver`/`JdkResolver` and
Maven `DownloadManager`/`JavaJDKManager` paths named in `Surprises & Discoveries`.

### Limitations, Remaining Work, and Open Questions

Providers currently describe concrete archive metadata; network API adapters and
plugin migration remain for later plans. Cross-host execution was simulated for
layout paths but not run on Windows or Linux in this environment. The core does
not yet expose Gradle/Maven preview or deploy workflows.

### Possible Article Angles

For build-tool maintainers: “Why a JDK cache should verify capabilities, not
just `bin/java`” demonstrates the selection diagnostics and process probe. For
platform engineers: “Atomic side-by-side developer tool installs across three
operating systems” explains roots, locks, checksums, and completion markers.

### Suggested Narrative

Start with duplicated resolver behavior and the risks of `latest` downloads,
then show the platform-neutral coordinate model, staged install path, timeout
and subprocess probe, the first closed-stream failure and fix, and the 8-test
publication result. Close with provider/network and plugin migration limits.

### Claims Requiring Human Review

The provider URL shapes are deliberately minimal metadata adapters and should be
reviewed before being treated as production download endpoints. The statement
that branch 392 contains the live-preview changes is user-directed and should be
verified against remote history before publication.

### Original Plan versus Actual Outcome

Not completed yet.

### What Changed

Not completed yet.

### Decisions and Trade-offs

Not completed yet.

### Unexpected Problems and Discoveries

Not completed yet.

### Validation and Measurable Results

Not completed yet.

### Useful Evidence and Examples

Not completed yet.

### Limitations, Remaining Work, and Open Questions

Not completed yet.

### Possible Article Angles

Not completed yet.

### Suggested Narrative

Not completed yet.

### Claims Requiring Human Review

Not completed yet.
