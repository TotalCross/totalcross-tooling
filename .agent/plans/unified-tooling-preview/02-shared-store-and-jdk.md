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

- [ ] Inventory current Gradle and Maven SDK/JDK resolution.
- [ ] Create the `tooling-java` build and `tooling-core` module.
- [ ] Implement native data/cache root resolution.
- [ ] Implement locked, checksum-verified, atomic installations.
- [ ] Implement concrete SDK and JDK metadata.
- [ ] Implement vendor-neutral JDK candidates and explicit overrides.
- [ ] Implement subprocess capability probes.
- [ ] Add focused tests for all supported host platforms.
- [ ] Publish the core artifact to Maven Local for later plans.
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

Not started.

## Revision Note

2026-07-26: separated store/JDK work into a bounded plan and required concrete
metadata plus real subprocess capability probes.

## Editorial Report

This section is mandatory at completion. Keep it factual and evidence-based.

### Editorial Summary

Not completed yet.

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
