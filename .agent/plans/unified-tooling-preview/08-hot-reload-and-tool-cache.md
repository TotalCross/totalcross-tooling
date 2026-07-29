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
- [ ] Replace placeholder tool metadata with concrete versions, URLs, and hashes.
- [ ] Install and verify `protoc` on supported host platforms.
- [ ] Install and verify `bundletool`.
- [ ] Pass resolved tools through `DeployToolchain`.
- [ ] Migrate Android deploy resolution to the shared store.
- [ ] Add read-only legacy SDK fallback with one deprecation diagnostic.
- [ ] Prove checksum failure, offline reuse, and concurrent installation safety.
- [ ] Remove obsolete SDK-local download code only after focused equivalence tests.
- [ ] Commit, update evidence, and set active plan to Plan 08B.

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

Reload coordination is implemented and tested. External-tool consumption by the
Android deployer remains the completion gate.

## Revision Note

2026-07-28: narrowed Plan 08 to the remaining concrete tool and Android deploy
work; release-level preview and plugin consolidation moved to Plan 08B.

## Editorial Report

Complete this section only from executed evidence.

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
