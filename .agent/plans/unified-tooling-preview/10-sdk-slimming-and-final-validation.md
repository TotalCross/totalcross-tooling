<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Complete post-IR SDK slimming and close the program

This ExecPlan is Plan 10. It starts after Plan 09 completes semantic IR
integration and the source-ownership decision.

## Purpose / Big Picture

Remove only payload proven obsolete after IR integration, preserve the pre-IR
support line, validate installation migration and compatibility, and close the
multi-repository program with measured outcomes.

## Working Set and Resume Protocol

Read state, this plan, the Plan-09 decision, and concise evidence. Inspect only
dependency declarations, package scripts, compatibility facades, cache migration,
and tests named by failures.

## Progress

- [ ] Inventory remaining SDK tooling classes, dependencies, and bundled tools.
- [ ] Compare the post-IR development artifacts with the pre-IR release baseline.
- [ ] Remove or relocate only consumers already migrated.
- [ ] Preserve compatibility facades and documented deprecations.
- [ ] Add legacy cache discovery or verified copy migration.
- [ ] Re-run aggregate SDK and narrow-artifact compatibility checks.
- [ ] Run the justified cross-project final matrix.
- [ ] Measure SDK and distribution size changes.
- [ ] Reconcile state, master, evidence, archive, and final report.
- [ ] Commit final documentation and leave both repositories scoped and clean.

## Current Architecture and Scope

The pre-IR release branch remains supported and unchanged. Plan 10 affects the
post-IR development line.

Candidates include deploy-only libraries, obsolete SDK-local tools, implementation
packages moved in Plan 09, and preview-host dependencies no longer required by
the aggregate SDK. Do not remove runtime Java classes, TCZ support, native VM
code, or compatibility facades without a separate major-version decision.

The aggregate SDK may remain published indefinitely. Narrow artifacts are
preferred by tooling but are not required for existing applications.


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

Generate a dependency and artifact inventory from the actual post-IR build. For
each removal candidate record:

    current artifact and path
    current consumers
    replacement
    focused proof
    size impact
    compatibility impact

Remove one capability group at a time. Update build scripts, publication
metadata, CI, and documentation together. Split oversized non-protected build
files before functional changes.

Implement legacy installation discovery for old Gradle, Maven, SDK-local, and
tooling caches. Use verified read-only access or copy into immutable shared-store
locations. Never delete old caches automatically.

Run focused compatibility tests for:

    totalcross.Launcher
    tc.Deploy
    aggregate totalcross-sdk.jar
    narrow SDK/runtime/converter/deployer/preview artifacts
    CLI preview and run
    Gradle package and continuous preview
    Maven package and preview
    VS Code wizard and Maven conversion rollback
    repeated worker reload
    global protoc and bundletool reuse
    IR default-off and enabled modes

Compare aggregate and distribution size with the Plan-03 and pre-IR release
baselines. Report bytes and percentages without claiming runtime performance.

Finalize the canonical editorial report with actual commands, results, skipped
hosts, publication status, compatibility decisions, IR merge findings, and
remaining major-version work.

## Decision Log

- Decision: slimming is post-IR and consumer-by-consumer.
  Rationale: release compatibility and IR integration must be known first.
  Date/Author: 2026-07-28 / OpenAI.

- Decision: pre-IR release branches are never rewritten by cleanup.
  Rationale: they are the stable support and rollback line.
  Date/Author: 2026-07-28 / OpenAI.

## Validation and Acceptance

A clean post-IR environment must resolve the toolchain, create or convert a
project, preview and reload through all supported entry points, package through
Gradle and Maven, reuse tools offline, and pass aggregate SDK compatibility
smokes.

Run focused tests, justified platform builds, `git diff --check`, and staged size
checks. Record every command, pass/fail status, log path, skipped validation, and
reason. Both repositories end with no unexplained changes.

## Risks and Open Questions

A dependency that appears deploy-only may still serve desktop runtime behavior.
Use class analysis and focused execution before removal. Some platform validation
may remain unavailable; report it honestly.

## Idempotence and Recovery

Each removal is separately revertible. Cache migration copies rather than moves.
Failed post-IR cleanup does not affect the pre-IR release branches or tags.

## Outcomes & Retrospective

Not started.

## Revision Note

2026-07-28: moved slimming after the pre-IR release and IR merge, preserved the
release line, and added measured comparison against both baselines.

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
