<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Slim the SDK and complete final compatibility validation

This ExecPlan is Plan 10 and closes the unified tooling and preview program.

## Purpose / Big Picture

Remove obsolete tooling payload from the SDK only after every supported consumer
uses the new artifacts and shared store. Prove old compatibility entry points,
new CLI/plugin/editor workflows, live reload, packaging, and installation
migration. Finish the factual editorial report from observed evidence.

## Working Set and Resume Protocol

Read state, this plan, master progress, and the concise evidence index. Inspect
only dependency declarations, package scripts, compatibility facades, install
migration, and tests named by failures. Do not reread completed plan files unless
their acceptance evidence is missing.

## Progress

- [ ] Inventory remaining SDK tooling classes, dependencies, and bundled tools.
- [ ] Remove or relocate only consumers already migrated.
- [ ] Preserve compatibility facades and documented deprecations.
- [ ] Add legacy cache discovery or one-time migration.
- [ ] Run focused compatibility and artifact-size checks.
- [ ] Run the justified cross-project final matrix.
- [ ] Reconcile state, master progress, evidence, archive, and outcomes.
- [ ] Finalize the editorial report with actual results.
- [ ] Commit final documentation and leave both repositories clean.

## Current Architecture and Scope

Candidates for removal from the aggregate SDK include deploy-only libraries,
converter/deployer implementation packages when ownership moved, preview-host
dependencies such as AWT helper libraries, and downloaded `etc/tools` content.
Do not remove runtime Java classes, TCZ support, native VM code, or compatibility
facades required by supported releases.

The final SDK may continue to publish an aggregate compatibility JAR while new
tooling consumes narrow artifacts. Complete removal of compatibility classes is
a future major-version decision unless this plan proves no supported consumer
requires them.


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

Generate a dependency and artifact inventory from the actual build. For each
candidate, name its current consumer and the new replacement. Remove it only when
a focused test proves the replacement. Keep removals in small commits grouped by
capability, not one broad cleanup.

Update packaging scripts and CI together with local build behavior. If an
oversized non-protected build or workflow file must change, extract reusable
sections before the functional edit. Do not modify generated projects or vendored
files.

Implement legacy installation discovery. The shared store may read old Gradle,
Maven, or SDK-local caches and either use them read-only or copy verified
artifacts into the new immutable layout. Never delete the old cache automatically.
Record source, checksum when available, and migration result.

Run focused compatibility tests for:

    totalcross.Launcher
    tc.Deploy
    aggregate totalcross-sdk.jar
    narrow SDK/runtime/converter/deployer/preview artifacts
    CLI preview and run
    Gradle package and continuous preview
    Maven package and preview
    VS Code project wizard
    VS Code Maven-to-Gradle conversion and rollback
    repeated worker reload
    global protoc and bundletool reuse

Then run only the platform/package matrix justified by changed build scripts.
Record unavailable hosts and credentials honestly. Measure final artifact sizes
and compare with the baseline from Plan 03; do not claim performance improvement
without a relevant measurement.

Consolidate completed detail into archive, rewrite state as complete, update the
master checklist, and finalize
`.agent/reports/unified-tooling-preview-editorial.md` with the required factual
sections. Copy only a concise final summary into each completed plan's Editorial
Report or point to the canonical report if repository rules allow.

## Surprises & Discoveries

- Observation: none recorded yet.
  Evidence: add final packaging or compatibility discoveries that matter to
  future maintenance.

## Decision Log

- Decision: remove SDK content only after consumer-by-consumer proof.
  Rationale: package size reduction must not break legacy workflows.
  Date/Author: 2026-07-26 / OpenAI.

- Decision: retain compatibility facades through this program.
  Rationale: implementation ownership can change without forcing an immediate
  user-facing breaking release.
  Date/Author: 2026-07-26 / OpenAI.

## Validation and Acceptance

The final acceptance scenario starts from a clean sample project and a shared
store with no selected SDK/JDK/tool version installed. It resolves the
toolchain, creates or converts a project in VS Code, starts preview, reloads after
a source edit, packages through Gradle and Maven samples, reuses cached tools
offline, and still runs the documented legacy facade smoke tests.

Run `git diff --check`, staged size-policy checks, repository-focused tests, and
the justified final builds. Record every command actually run, pass/fail status,
log path, skipped expensive validation, and reason. Both repositories end with
no unexplained changes.

## Risks and Open Questions

A dependency that appears deploy-only may also be used by desktop runtime paths.
Use class and dependency analysis plus focused execution before removal. CI
platform availability may limit proof; report unsupported or untested targets
instead of generalizing.

## Idempotence and Recovery

Each removal is separately revertible. Legacy cache migration copies rather than
moves. Package scripts write to build outputs. If the final matrix exposes a
regression, revert the smallest capability commit and keep the compatibility
artifact until a follow-up plan.

## Outcomes & Retrospective

Not started. At completion summarize delivered workflows, retained compatibility,
measured artifact changes, deferred platforms, and lessons from the IR merge.

## Revision Note

2026-07-26: made SDK slimming evidence-driven and added final cross-project,
installation, editor, build-tool, and editorial completion criteria.

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
