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
`feature/422-create-ir-for-jniaot`. The release preserves `totalcross-sdk.jar`,
`totalcross.Launcher`, and `tc.Deploy`; keeps converter and native IR ownership
unchanged; and ships one process-isolated preview lifecycle through CLI, Gradle,
Maven, and VS Code.

The VS Code extension preserves its Gradle wizard and Maven migration, supports
per-project reminder suppression, and can request conversion of an unstructured
legacy Java/TotalCross folder into a validated Gradle Java project. Conversion
analysis and mutation are implemented by shared Java tooling used by the Gradle
plugin and CLI; VS Code owns only selection, confirmation, progress, and result
presentation.

## Working Set and Resume Protocol

Read first:

    .agent/state/unified-tooling-preview.md

Read this master only to select the next plan. Then read the active plan in full.
Search evidence only when an earlier command result is required.

Repositories and branches:

    <workspace>/totalcross
      feature/392-feature-request-live-ui-preview-for-ides

    <workspace>/totalcross-tooling
      feature/unify-tooling-and-preview

Release branches may be created only after Plan 08D acceptance. Pushes, tags,
publications, and releases require explicit user approval.

## Progress

- [x] Plan 01: bootstrap repositories, plans, state, and workspace.
- [x] Plan 02: shared store and vendor-neutral JDK policy.
- [x] Plan 03: logical artifacts and typed deploy boundary.
- [x] Plan 04: launcher decomposition and runtime preview boundary.
- [x] Plan 05: protocol, host, worker, and CLI structural checkpoint.
- [x] Plan 06: Gradle and Maven structural integration checkpoint.
- [x] Plan 07: VS Code workflow preservation checkpoint.
- [x] Plan 08: shared external tools and Android deploy migration.
- [x] Plan 08B: first complete Gradle/Maven/VS Code E2E checkpoint.
- [ ] Plan 08C: correct preview, model, resolver, and plugin architecture.
- [ ] Plan 08D: add shared legacy-project conversion and reminder suppression.
- [ ] Plan 08R: stage, publish, and verify the pre-IR release.
- [ ] Plan 09: merge IR and decide physical source ownership.
- [ ] Plan 10: post-IR slimming and final program closure.
- [ ] Reconcile state, evidence, archive, and editorial report.

Plans 05–08B remain valid evidence checkpoints. A post-completion audit found
release blockers isolated in Plans 08C and 08D rather than rewriting history.

## Pre-IR release scope

The release includes:

    aggregate totalcross-sdk.jar and additive narrow artifacts
    Launcher and Deploy compatibility facades
    shared SDK, JDK, and external-tool store
    typed deploy as the only public plugin package path
    authenticated protocol and disposable worker candidates
    promotion only after ready plus first valid frame
    distinct standalone preview and run modes
    equivalent Gradle and Maven models and commands
    VS Code Gradle project wizard
    Maven-to-Gradle conversion with rollback
    per-project Maven reminder suppression
    shared Convert to TotalCross Project engine
    Gradle task for conversion in existing Gradle builds
    CLI conversion for folders that are not yet Gradle projects
    bundled or checksummed companion delivery

The release excludes:

    IR branch merge
    TCIR, JIT, or AOT relocation
    TCZ format changes
    physical converter source movement
    removal of totalcross-sdk.jar, Launcher, or Deploy
    aggressive SDK slimming not proven compatible


## Cross-plan safety and size policy

Run one plan at a time and preserve unrelated work. Never use `git reset --hard`,
`git clean -fd`, force-push, history rewriting, tag deletion, or repository
archival unless the user explicitly requests that exact operation.

Every implementation text file created or modified in the TotalCross repositories
must remain at or below 20 KiB and approximately 600 lines. Run the staged
size-policy checker before every commit. Split an oversized non-protected file by
responsibility before changing its behavior. Do not split a protected IR file
merely to satisfy this rule.

Protected paths:

    TotalCrossSDK/src/main/java/tc/tools/converter/**
    TotalCrossSDK/src/test/java/tc/tools/converter/**
    TotalCrossVM/src/tcvm/ir/**
    TotalCrossVM/src/tcvm/jit/**
    TotalCrossVM/src/tcvm/aot/**
    TotalCrossVM/src/tests/ir/**
    docs/architecture/bytecode/**

A project-conversion operation may relocate a pre-existing user file without
rewriting or splitting its contents, even when that file is larger. Newly
generated build files, reports, journals, and implementation files remain
subject to the limit. Split a large migration journal into numbered chunks.

Generated build output, caches, downloaded tools, credentials, and third-party
content must not be committed.

Use token-efficient execution. Read the state file and active plan first. Inspect
only named paths. Save verbose logs under `/tmp` or build artifacts and record
only concise outcomes, commit IDs, and log paths. Do not repeatedly print full
plans, diffs, generated projects, dependency trees, or test logs.

## Plan of Work

Plan 08C connects the promotion coordinator to real worker processes, preserves
the active application after failed builds or candidates, makes shared store/JDK
selection authoritative, serializes the complete project model, removes
duplicate public package paths, and corrects VS Code reload, input, resize,
wrapper, and packaging behavior.

Plan 08D creates a shared Java conversion module. The Gradle plugin exposes that
engine as `totalcrossConvertProject` when a Gradle build already exists. The
tooling CLI exposes the same engine for arbitrary folders that cannot load a
Gradle plugin yet. VS Code calls the CLI and displays its structured plan and
results; it does not implement source classification, script parsing, version
selection, file movement, validation, or rollback.

Plan 08D also adds “Don't Ask Again for This Project” to the automatic Maven
conversion reminder and a command that re-enables the reminder for one selected
project.

Plan 08R starts only after Plan 08D passes. It chooses non-SNAPSHOT versions,
creates release branches, publishes to staging, consumes from empty caches and a
fresh shared store, and, after explicit approval, publishes SDK artifacts,
tooling modules, Maven plugin, Gradle plugin, companion, and VSIX in dependency
order.

Plan 09 starts only after Plan 08R records a verified pre-IR release. It merges
the reviewed IR branch, revalidates converter/native fixtures, and decides
whether physical converter or deployer movement is safe.

Plan 10 removes only post-IR payload proven obsolete, completes cache migration,
measures artifact changes, and closes the program.

## Decision Log

- Decision: add Plans 08C and 08D instead of reopening historical plans.
  Rationale: prior tests remain useful evidence, while the audit findings require
  explicit new release gates.
  Date/Author: 2026-07-29 / User and OpenAI.

- Decision: a reload replaces the worker process, not only the MainWindow.
  Rationale: process replacement is the reliable boundary for threads, statics,
  native resources, timers, and application classloaders.
  Date/Author: 2026-07-29 / OpenAI.

- Decision: the conversion engine belongs to shared Java tooling.
  Rationale: the Gradle plugin can expose it after a build exists, while a CLI is
  required to convert a folder that cannot load the plugin yet. VS Code remains
  a thin consumer in both cases.
  Date/Author: 2026-07-29 / User and OpenAI.

- Decision: legacy conversion is dry-run-first and transactional.
  Rationale: source-layout inference and script analysis are heuristic and must
  never silently destroy a working project.
  Date/Author: 2026-07-29 / User and OpenAI.

- Decision: SDK and Java fallbacks use shared dynamic policies.
  Rationale: latest available SDK and highest accepted Java target change over
  time and must not be hardcoded in the extension.
  Date/Author: 2026-07-29 / User and OpenAI.

## Validation and Acceptance

The pre-IR release is accepted when a clean environment resolves all staged
artifacts without `mavenLocal`, starts preview from CLI, Gradle, Maven, and VS
Code, preserves the old worker after failed compilation or candidate startup,
promotes a valid new process after its first frame, stops all owned processes,
packages through typed deploy, and reuses installed tools offline.

An installed VSIX must create a project, convert Maven to Gradle, suppress the
automatic reminder for one project without suppressing another, invoke the
shared converter for representative legacy folders, preserve detected Launcher
and Deploy arguments, and report rollback after forced validation failure.

An existing project depending only on `com.totalcross:totalcross-sdk` must still
compile and pass focused Launcher and deploy smoke tests.

## Risks and Open Questions

A folder without `settings.gradle` or `build.gradle` cannot apply a Gradle plugin
normally. Do not use a fragile generated init script as the primary conversion
path. The CLI is the bootstrap entry point; the Gradle task is the native entry
point after Gradle exists.

Inference from scripts is conservative. Ambiguous roots, multiple MainWindow
candidates, or conflicting versions require user selection rather than a guess.

## Idempotence and Recovery

Each plan verifies branch, origin, working-tree scope, last checkpoint, and state.
Failed conversion or staging does not alter public repositories. Conversion
maintains a hash-verified journal and rollback backup. Release branches receive
only release fixes and remain separate from Plan 09.

## Outcomes & Retrospective

Plans 01–08B produced the workspace, shared core, artifact boundaries, launcher
split, tooling modules, build-tool adapters, VS Code workflows, Android tool
migration, and an installed-project E2E. The audit found that production reload
still reused one worker and that resolver, model, extension, and release details
remained incomplete. Plan 08C is active.

## Revision Note

2026-07-29: inserted Plan 08C for architectural corrections and Plan 08D for a
shared Gradle/CLI conversion engine plus project-scoped reminder suppression.


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
