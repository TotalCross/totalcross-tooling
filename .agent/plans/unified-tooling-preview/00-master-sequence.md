<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Coordinate the TotalCross tooling and live-preview program

This is the master ExecPlan for a sequential multi-repository program. It follows
`TotalCross/totalcross-depot-tools/.agent/PLANS.md`. Detailed work is split into
bounded plans so a lower-capacity model can execute one context at a time.

## Purpose / Big Picture

Deliver a publishable TotalCross release before merging
`feature/422-create-ir-for-jniaot`. The pre-IR release keeps converter and native
IR ownership unchanged, preserves `totalcross-sdk.jar`, `totalcross.Launcher`,
and `tc.Deploy`, and ships one production preview lifecycle through CLI, Gradle,
Maven, and VS Code.

After the pre-IR release is tagged and verified, development may continue with
the IR merge, source-ownership decision, and final SDK slimming.

## Working Set and Resume Protocol

Read first:

    .agent/state/unified-tooling-preview.md

Read this master only to choose the next plan. Then read the active plan in full.
Search evidence only when a prior command result is needed.

Repositories:

    <workspace>/totalcross
    <workspace>/totalcross-tooling

Branches:

    totalcross:
      feature/392-feature-request-live-ui-preview-for-ides

    totalcross-tooling:
      feature/unify-tooling-and-preview

The pre-IR release must be cut from dedicated release branches created only after
Plan 08B acceptance. Pushes, tags, publication, and releases require explicit
user approval at the irreversible step.

## Progress

- [x] Plan 01: bootstrap repositories, plans, state, and workspace.
- [x] Plan 02: shared store and vendor-neutral JDK policy.
- [x] Plan 03: logical artifacts and typed deploy boundary.
- [x] Plan 04: launcher decomposition and runtime preview boundary.
- [x] Plan 05: protocol, host, worker, and CLI structural checkpoint.
- [x] Plan 06: Gradle and Maven structural integration checkpoint.
- [x] Plan 07: VS Code workflow preservation checkpoint.
- [x] Plan 08: complete shared tools and Android deploy migration.
- [x] Plan 08B: stabilize one production preview and plugin/editor flow.
- [ ] Plan 08R: stage, publish, and verify the pre-IR release.
- [ ] Plan 09: merge IR and decide physical source ownership.
- [ ] Plan 10: post-IR slimming and final program closure.
- [ ] Reconcile state, evidence, archive, and editorial report.

Plans 05–07 being checked means their modules and focused tests exist. Their
release-level end-to-end acceptance is intentionally closed by Plan 08B.

## Pre-IR release scope

The release includes:

    aggregate totalcross-sdk.jar
    narrow SDK artifacts published in parallel when ready
    totalcross.Launcher compatibility facade
    tc.Deploy compatibility facade
    shared SDK/JDK/tool store
    vendor-neutral tooling JDK selection
    typed deploy as the plugin execution path
    authenticated preview protocol
    persistent host and disposable worker
    standalone preview/run CLI
    Gradle preview, run, stop, and package
    Maven preview, run, stop, and package
    VS Code Gradle project wizard
    VS Code Maven-to-Gradle conversion with rollback
    one VS Code preview command set
    automatic companion discovery or installation

The release excludes:

    IR branch merge
    TCIR, JIT, or AOT relocation
    TCZ format changes
    physical converter source movement
    converter fixture ownership changes
    removal of totalcross-sdk.jar
    removal of totalcross.Launcher or tc.Deploy
    breaking Launcher API changes
    aggressive SDK slimming not proven compatible


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

Plan 08 finishes the work already active: concrete external-tool metadata,
`protoc` and `bundletool` installation, Android deploy integration through
`DeployToolchain`, offline reuse, and safe legacy fallback.

Plan 08B makes the `tooling-java` host/worker stack the only production preview
lifecycle. The HTTP/Webview server becomes an adapter or is marked legacy. It
completes the CLI, Gradle, Maven, and VS Code flows; removes public placeholders;
makes shared JDK/SDK/Java policy authoritative; resolves Maven JVM compatibility;
and proves compatibility with the aggregate SDK.

Plan 08R freezes release branches, chooses non-SNAPSHOT versions, publishes to a
staging repository, consumes the staged artifacts from empty caches, and then,
after explicit approval, publishes SDK artifacts, tooling modules, Maven plugin,
Gradle plugin, companion distribution, and VSIX in dependency order. It publishes
a beta first unless prior external validation justifies an RC.

Plan 09 starts only after Plan 08R records a successful pre-IR release. It merges
the reviewed IR work, revalidates converter/native fixtures, and decides whether
physical converter/deployer movement is safe.

Plan 10 removes only post-IR payload proven obsolete, completes cache migration,
runs final compatibility measurements, and closes the long-running program.

## Decision Log

- Decision: cut a supported pre-IR release before branch 422 integration.
  Rationale: preview/tooling can be stabilized independently and provide a known
  rollback point before converter and VM changes.
  Date/Author: 2026-07-28 / User and OpenAI.

- Decision: close release gaps in Plan 08B instead of reopening Plans 05–07.
  Rationale: prior checkpoints and evidence remain valid while the new plan
  explicitly distinguishes scaffolding from production acceptance.
  Date/Author: 2026-07-28 / OpenAI.

- Decision: keep the aggregate SDK during the pre-IR release.
  Rationale: existing projects must not be forced to declare narrow artifacts.
  Date/Author: 2026-07-28 / OpenAI.

- Decision: publish a beta before an RC unless equivalent external staging
  evidence already exists.
  Rationale: the release introduces new cross-process and multi-tool workflows.
  Date/Author: 2026-07-28 / OpenAI.

## Validation and Acceptance

The pre-IR release is accepted when a clean environment can resolve all public
artifacts without `mavenLocal`, create or convert a project in VS Code, start
preview from CLI/Gradle/Maven/VS Code, rebuild after a source edit, preserve the
old worker after a failed build or candidate, promote a valid replacement after
its first frame, stop the session, package through the typed deploy path, and
reuse installed tools offline.

An existing project depending only on `com.totalcross:totalcross-sdk` must still
compile and pass focused `Launcher` and `tc.Deploy` smoke tests. Preview may
require the new SDK version and must report that requirement clearly.

## Risks and Open Questions

The current repository contains two preview paths. Plan 08B must remove lifecycle
duplication before release. The Maven plugin currently mixes an older target with
Java-17 tooling classes; Plan 08B must either isolate tooling in a Java-17
subprocess or document and version a Java-17 minimum.

Narrow artifacts are not replacements for the aggregate SDK until their API
coverage and dependency metadata are proven. Publish them as additive artifacts.

## Idempotence and Recovery

Every plan verifies branch, origin, working-tree scope, last checkpoint, and
state. Failed staging does not change public repositories. Release branches are
created only after Plan 08B acceptance and receive only release fixes. The feature
branches remain available for Plan 09.

## Outcomes & Retrospective

Plans 01–07 produced the workspace, shared core, artifact boundaries, launcher
split, preview modules, plugin scaffolding, and preserved VS Code workflows.
Plan 08B completed the release-level integration slice, including the clean
Gradle/Maven matrix and trusted installed VS Code project E2E. Plan 08R is now
active for staging and pre-IR release verification; public publication remains
explicitly out of scope until approved.

## Revision Note

2026-07-29: closed Plan 08B after the trusted installed VS Code E2E passed with
the rebuilt production VSIX; transitioned the resumable state to Plan 08R.

2026-07-28: inserted Plan 08B and Plan 08R, defined a pre-IR release boundary,
and deferred IR integration until the published release is verified.

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
