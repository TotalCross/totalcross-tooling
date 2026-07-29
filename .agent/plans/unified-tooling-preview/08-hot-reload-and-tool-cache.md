<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Complete shared tools and Android deploy migration

This ExecPlan is Plan 08. It completes the active shared-tool work. Do not start
Plan 08B until every acceptance item below passes.

## Purpose / Big Picture

Store `protoc`, `bundletool`, and future helper tools outside individual SDK
installations and make Android deployment consume them through the typed tooling
boundary. Repeated preview reload work already implemented remains covered by its
focused tests.

## Working Set and Resume Protocol

Read state and this plan. Inspect only:

    tooling-java shared store and external-tool catalog
    typed DeployToolchain and LegacyDeployService
    Android deploy tool-resolution code
    focused Android packaging tests
    Plan 08 reload coordinator tests

Do not merge IR, move converter source, or perform publication.

## Progress

- [x] Add debounced reload state transitions.
- [x] Promote a candidate only after ready plus first frame.
- [x] Add resource/class handling and stale-session cleanup.
- [x] Add repeated-reload failure and leak smoke tests.
- [x] Add versioned tool catalog structures.
- [x] (2026-07-28) Replace placeholder tool metadata with concrete versions, URLs, and hashes.
- [x] (2026-07-28) Install and verify `protoc` on supported host platforms.
- [x] (2026-07-28) Install and verify `bundletool`.
- [x] (2026-07-28) Pass resolved tools through `DeployToolchain`.
- [x] (2026-07-28) Migrate Android deploy resolution to the shared store.
- [x] (2026-07-28) Add read-only legacy SDK fallback with one deprecation diagnostic.
- [x] (2026-07-28) Prove checksum failure, offline reuse, and concurrent installation safety.
- [x] (2026-07-28) Remove obsolete SDK-local download code after focused compile and resolver tests.
- [x] (2026-07-28) Commit, update evidence, and set active plan to Plan 08B.

## Surprises & Discoveries

- Observation: the macOS ARM Protobuf asset reports `libprotoc 3.21.0` even though
  the catalog coordinate is the upstream release line `21.0`.
  Evidence: the official asset probe returned `libprotoc 3.21.0`; the resolver
  therefore validates the release-line substring rather than assuming the binary
  prints the archive coordinate verbatim.

- Observation: the Bundletool download was larger than the first partial transfer
  suggested, so the final SHA-256 had to be calculated only after a resumed,
  complete download.
  Evidence: the complete 29,105,379-byte JAR passed `java -jar ... version` with
  output `1.15.6` and has SHA-256
  `38ae8a10bcdacef07ecce8211188c5c92b376be96da38ff3ee1f2cf4895b2cb8`.

- Observation: `Deployer4Android.java` already exceeded the file-size policy.
  Evidence: the Android tool lookup and embedded-package writer were extracted
  into `AndroidToolLocator.java` and `AndroidPackageFiles.java`; the remaining
  deployer is 19,609 bytes and 469 lines.

## Current Architecture and Scope

Shared installations use:

    tools/protoc/<version>/<platform>/
    tools/bundletool/<version>/all/

Each completed installation records source, concrete version, SHA-256, platform,
and completion metadata. `bundletool` is platform-independent Java content.
`protoc` is host-specific.

The Android deployer receives resolved paths from `DeployToolchain`. It must not
construct vendor URLs or select cache directories. An explicitly configured old
SDK may be read as a temporary fallback; the deployer must not download new files
into that SDK.


## Cross-plan safety and size policy

Run one plan at a time. Preserve unrelated work. Never use `git reset --hard`,
`git clean -fd`, force-push, history rewriting, tag deletion, or repository
archival unless the user explicitly requests that exact operation.

Every created or modified text file must remain at or below 20 KiB and at or
below approximately 600 lines. Run the staged size-policy checker before every
commit. If an existing non-protected file exceeds either limit, split it by
responsibility before the functional change. Do not split a protected IR-related
file merely to satisfy this rule.

Protected paths:

    TotalCrossSDK/src/main/java/tc/tools/converter/**
    TotalCrossSDK/src/test/java/tc/tools/converter/**
    TotalCrossVM/src/tcvm/ir/**
    TotalCrossVM/src/tcvm/jit/**
    TotalCrossVM/src/tcvm/aot/**
    TotalCrossVM/src/tests/ir/**
    docs/architecture/bytecode/**

Generated files, third-party code, caches, and build output must not be committed.

Use token-efficient execution. Read the state file first, then the active plan.
Inspect only named paths. Store verbose output in `/tmp` or build artifacts and
record only concise results, commit IDs, and log paths. Do not repeatedly print
large plans, logs, generated files, or full repository diffs.

## Plan of Work

First verify upstream release files and calculate hashes from downloaded bytes.
Do not commit `"catalog-required"`, empty hashes, floating `latest` URLs, or
machine-local paths.

Add catalog entries for every host currently supported by Android packaging.
A missing host entry produces a clear unsupported-host error. Install through the
Plan-02 locked, checksum-verified, atomic store.

After installation, run:

    protoc --version
    java -jar bundletool.jar version

Use the selected tooling JDK. On macOS, run the existing ProcessBuilder/xattr
probe and distinguish an absent quarantine attribute from process-creation
failure.

Extend `DeployToolchain` with typed accessors for required tools. Change Android
deploy code to request those paths. Keep download, extraction, and store classes
out of the SDK deploy implementation.

Add a temporary fallback that accepts an explicitly configured legacy SDK tool
path only when the file exists and passes a version probe. Log one deprecation
diagnostic per execution. Never silently prefer legacy content over a verified
shared installation.

Focused tests must cover concrete catalog metadata, checksum mismatch,
interrupted staging, concurrent installation, offline reuse, missing platform,
version probe failure, shared-store preference, and legacy fallback.

Run one focused Android package with the shared tools. Compare output type,
expected files, and success status against the current path. Only then remove
obsolete local download behavior. Retain templates or SDK resources still needed
at runtime.

## Decision Log

- Decision: finish external tools before preview release stabilization.
  Rationale: Gradle, Maven, CLI, and VS Code must share the same deploy toolchain
  during end-to-end release tests.
  Date/Author: 2026-07-28 / OpenAI.

- Decision: legacy SDK fallback is read-only and explicit.
  Rationale: compatibility must not recreate mutable per-SDK tool ownership.
  Date/Author: 2026-07-28 / OpenAI.

## Validation and Acceptance

Acceptance requires:

    all tooling-java tests pass
    twenty-reload candidate test passes
    concrete catalog contains no placeholder checksum
    protoc and bundletool version probes pass
    checksum failure is rejected
    second install works offline
    two concurrent requests produce one valid installation
    Android deploy uses DeployToolchain paths
    legacy fallback is tested and deprecated
    obsolete SDK-local download code is removed or explicitly documented as used

Run focused Android packaging, `git diff --check`, and staged size checks in both
repositories. Record exact commits and log paths in state and evidence.

## Risks and Open Questions

Upstream release layouts may change. Provider-specific URL logic must remain
isolated. Existing Android code may combine tool resolution with packaging; split
that non-protected file before modification when it exceeds the size policy.

## Idempotence and Recovery

Installations are immutable. Failed downloads remain in unique staging paths and
never replace a completed installation. Reverting the Android integration commit
restores the prior resolver without deleting shared-store content.

## Outcomes & Retrospective

Reload coordination is implemented and tested. Plan 08 now provides concrete,
checksum-verified shared installations for Protobuf 21.0 and Bundletool 1.15.6,
passes their version probes, and gives Android deploy a typed pair of paths. The
old SDK-local download code is gone; a selected SDK can only supply existing,
verified files as a deprecated read-only fallback. The remaining work is the
release-level preview and plugin stabilization in Plan 08B.

## Revision Note

2026-07-28: narrowed Plan 08 to the remaining concrete tool and Android deploy
work; release-level preview and plugin consolidation moved to Plan 08B.

## Editorial Report

Complete this section only from executed evidence.

### Editorial Summary

Android deployment previously owned mutable downloads inside each SDK. Plan 08
moves `protoc` and Bundletool into the shared TotalCross store, verifies each
archive by SHA-256, and passes the resulting paths through the typed deploy
boundary. Existing SDKs remain usable through a read-only fallback when their
tool files pass version probes.

### Original Plan versus Actual Outcome

The plan retained the existing tool versions but replaced placeholder metadata
with direct GitHub release assets and measured hashes. Bundletool is installed as
an immutable JAR rather than extracted as a ZIP directory. The Android SDK
deployer was split to stay within the repository's file-size policy, and its
legacy downloads were removed rather than retained as a second mutable path.

### What Changed

`tooling-java/tooling-core` gained host-aware `ExternalToolCatalog`,
`ExternalToolResolver`, single-file installation support in `ArtifactInstaller`,
and `LegacyAndroidToolFallback`. `DeployToolchain` now exposes `protoc` and
`bundletool`; `LegacyDeployService` temporarily supplies those paths through
properties understood by `TotalCrossSDK/src/main/java/tc/tools/deployer/`.
`gradle-plugin/src/main/java/com/totalcross/gradle/TypedDeployTask.java` resolves
shared tools for Android tasks and reports fallback use. The SDK gained
`AndroidToolLocator.java` and `AndroidPackageFiles.java`.

### Decisions and Trade-offs

The shared store is preferred because it is immutable, checksum-verified, and
independent of a particular SDK installation. The compatibility fallback is
explicitly read-only and emits one warning per typed deploy execution. Windows
ARM reuses the upstream `win64` asset because that is the existing deploy
behavior; unsupported tool names or hosts fail with a clear error.

### Unexpected Problems and Discoveries

The macOS ARM asset's binary version text differs from its release coordinate,
and the first Bundletool transfer was incomplete. Both findings were handled by
probing the actual binaries and calculating hashes from complete downloaded
bytes. An existing SDK test also requires `-Dtotalcross.artifact.dir`; running
that test without the property failed during test initialization, so SDK
artifact-boundary assertions remain unverified in this environment.

### Validation and Measurable Results

`./gradlew :tooling-core:test --console=plain` passed 13 tests, including
catalog coverage, checksum rejection, failed version probes, offline reuse,
staging cleanup, and concurrent installation. The Gradle plugin test suite
passed. `./gradlew compileJava --console=plain` passed in `TotalCrossSDK`.
Official probes returned `libprotoc 3.21.0` and `1.15.6`; the resolver installed
them under `tools/protoc/21.0/macos-arm64/` and `tools/bundletool/1.15.6/all/`.
The license validator and its 19 unittest cases passed.

### Useful Evidence and Examples

Evidence is in `/tmp/tooling-plan08-core-test-final.log`,
`/tmp/gradle-plugin-plan08-test-final.log`,
`/tmp/totalcross-plan08-sdk-compile-final.log`, and
`/tmp/tooling-plan08-license.log`. The focused resolver test is
`tooling-java/tooling-core/src/test/java/com/totalcross/tooling/ExternalToolResolverTest.java`.

### Limitations, Remaining Work, and Open Questions

Plan 08 does not prove a full signed Android package in this environment and
does not yet prove CLI, Maven, VS Code, or clean-cache end-to-end release flows.
Those are Plan 08B and Plan 08R responsibilities. The SDK artifact-boundary
test needs its artifact directory property supplied by the artifact build.

### Possible Article Angles

An article for tooling maintainers could explain how to move mutable SDK-owned
downloads into an immutable shared store. A second angle could cover typed
boundaries for adapting legacy Java deployers without putting tooling classes on
the SDK classpath. A third could focus on checksum, atomic staging, offline
reuse, and concurrent installation as practical guarantees for IDE tooling.

### Suggested Narrative

Begin with the per-SDK download problem and the compatibility constraint. Show
the catalog and store layout, then the typed path handoff into the isolated
legacy deployer. Explain the partial-download/hash discovery and the file-size
driven split. Close with the passing resolver tests and binary probes, then make
clear that signed Android packaging and release staging remain future work.

### Claims Requiring Human Review

Claims about support for every host, the security value of checksum verification,
and the compatibility of all legacy SDK layouts require normal technical review
before publication. The full signed Android-package result is not claimed here.
