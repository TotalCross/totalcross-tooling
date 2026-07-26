<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Coordinate the TotalCross tooling and live-preview program

This is the master ExecPlan for a sequential multi-repository program. It follows
the rules in `TotalCross/totalcross-depot-tools/.agent/PLANS.md`. After bootstrap,
it is maintained under
`totalcross-tooling/.agent/plans/unified-tooling-preview/00-master-sequence.md`.
The detailed implementation is intentionally split into smaller ExecPlans so a
lower-capacity model can execute one bounded context at a time.

## Purpose / Big Picture

After this program, a developer can install and use TotalCross tooling without
depending on tooling classes bundled inside the SDK. A standalone Java live
preview can start from the command line, Gradle, Maven, or VS Code; source changes
can rebuild and replace an isolated preview worker; the VS Code extension still
owns its Gradle project wizard and Maven-to-Gradle conversion workflow; SDKs,
JDKs, and external tools are installed side by side in operating-system-native
locations.

The program first creates a reproducible workspace from an otherwise empty
folder. It then separates artifacts before moving source, so the unfinished
`feature/422-create-ir-for-jniaot` work is not disrupted. Physical converter and
deployer ownership is reconsidered only after that branch has been merged and
its converter-to-native fixtures have been revalidated.

## Working Set and Resume Protocol

Execute the numbered plans in order. The normal first read after bootstrap is:

    totalcross-tooling/.agent/state/unified-tooling-preview.md

Read this master plan only to select the next numbered plan or reconcile a
milestone boundary. Read the active numbered plan in full, then inspect only the
paths it names. Search
`.agent/evidence/unified-tooling-preview.md` only when prior command evidence is
needed. Completed detail moves to
`.agent/archive/unified-tooling-preview-history.md`. The final factual handoff is
`.agent/reports/unified-tooling-preview-editorial.md`.

The repositories are sibling directories:

    <workspace>/totalcross
    <workspace>/totalcross-tooling

The TotalCross branch is
`feature/392-feature-request-live-ui-preview-for-ides`. The tooling repository is
cloned from remote `main`, then uses
`feature/unify-tooling-and-preview`. Local commits may be created by the plans.
Pushes, pull requests, tags, releases, and destructive history operations require
explicit user instruction.

## Progress

- [x] Execute Plan 01 and create the canonical workspace and plan set.
- [x] Execute Plan 02 and deliver the shared store and vendor-neutral JDK policy.
- [ ] Execute Plan 03 and establish artifact and deploy boundaries without moving converter sources.
- [ ] Execute Plan 04 and decompose the desktop launcher before extending the preview contract.
- [ ] Execute Plan 05 and deliver the standalone preview host, worker, protocol, and CLI.
- [ ] Execute Plan 06 and integrate Gradle and Maven build flows.
- [ ] Execute Plan 07 and preserve and extend the VS Code workflows.
- [ ] Execute Plan 08 and complete hot reload plus reusable external-tool storage.
- [ ] Execute Plan 09 only after the IR merge gate is satisfied.
- [ ] Execute Plan 10 and finish SDK slimming, compatibility, and final validation.
- [ ] Reconcile the master outcomes and final editorial report.

## Current Architecture and Scope

The `totalcross` repository contains the Java SDK, the native VM, the current
desktop launcher, converter, deployer, platform packaging, and the branch-392
preview work. The `totalcross-tooling` repository contains the Gradle plugin,
Maven plugin, VS Code extension, and related shared tooling work.

The current preview contract is not process-neutral because it exposes AWT
`BufferedImage` and TotalCross UI types. The current plugins duplicate SDK/JDK
selection and Java compatibility logic. Android deployment downloads tools such
as `protoc` and `bundletool` beneath the SDK. The VS Code extension already has
valuable user-facing project creation and migration behavior that must remain.

This program does not move native TCIR, JIT, or AOT implementation out of the
VM. It does not change TCZ format as a prerequisite. It does not make preview
worker isolation optional for production use. It does not remove compatibility
entry points `totalcross.Launcher` or `tc.Deploy` until a later major-version
decision.


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

Plan 01 bootstraps both repositories, records immutable baselines, creates the
multi-root VS Code workspace, installs the plan set in the tooling repository,
and creates the shared state, evidence, archive, report, and file-size checker.

Plan 02 creates a pure-Java shared tooling core. It implements operating-system
native data and cache roots, immutable side-by-side SDK/JDK/tool installations,
checksums, file locks, atomic staging, vendor-neutral JDK resolution, and real
process capability probes. Corretto is a macOS candidate, not an assumption;
the selected JDK must prove that ordinary subprocesses, `xattr`, and `protoc`
can be launched.

Plan 03 creates separate SDK artifacts from the current source locations and a
typed deploy API in tooling. It keeps the converter and deployer source in
`totalcross`, preserves the legacy aggregate SDK JAR, and makes new consumers use
narrow artifacts and adapters.

Plan 04 first splits the oversized `totalcross.Launcher` by responsibility,
because modified non-protected files must meet the size policy. It then adds a
runtime-facing preview adapter that can copy frames and accept lifecycle/input
commands without exposing AWT or TotalCross UI classes in the tooling protocol.

Plan 05 creates a loopback-only authenticated protocol, disposable preview
worker, AWT host, and CLI commands. A standalone command starts an application,
shows frames, reports diagnostics, and exits cleanly.

Plan 06 makes Gradle and Maven emit the same build-session model. Gradle uses
continuous build to recompile and notify a persistent host. Maven owns a bounded
watch loop that invokes its compile lifecycle. Shared SDK/JDK, target-version,
and Retrolambda policy leaves the plugin implementations.

Plan 07 keeps the VS Code extension responsible for the project wizard,
Maven-to-Gradle conversion, backup and rollback presentation, workspace opening,
preview UI, device settings, and diagnostics. It delegates build and preview
mechanics to the shared tooling.

Plan 08 adds worker replacement, debounce, stale-session cleanup, resource
reload, and the global versioned storage of `protoc`, `bundletool`, and similar
tools. It proves repeated reload without retaining old application classloaders.

Plan 09 detects whether `feature/422-create-ir-for-jniaot` is merged into the
integration base. If not, it stops without altering protected files. If merged,
it integrates the base into branch 392, revalidates TCIR and converter fixtures,
creates a repository-preserving migration branch, and moves only the agreed
converter/deployer ownership while keeping native TCIR/JIT/AOT in `totalcross`.

Plan 10 removes obsolete aggregate dependencies and SDK-bundled tools only after
all new consumers pass. It validates compatibility facades, installation
migration, plugin workflows, VS Code workflows, preview behavior, packaging, and
the final file-size policy.

## Surprises & Discoveries

- Observation: the remote default branch of `TotalCross/totalcross-tooling` is
  `main`, even though an older local checkout may call its branch `master`.
  Evidence: record `git remote show origin` during Plan 01.

- Observation: branch 392 already exposes preview functionality through AWT and
  TotalCross UI types, so extraction must preserve behavior while changing the
  external boundary.
  Evidence: inspect `TotalCrossSDK/src/main/java/totalcross/preview/PreviewRuntime.java`
  in Plan 04.

- Observation: the IR branch uses converter-generated native fixtures, which
  makes a premature source move a merge and validation risk.
  Evidence: record the exact branch diff and fixture paths during Plan 01.

- Observation: the TotalCross remote default branch is `master`, not `main`.
  Evidence: `origin/HEAD` points to `origin/master` at
  `b7c25d7762aa326bf0c3a9bd384c173efad006da`; `origin/main` does not exist.

- Observation: the live-preview integration base is the existing branch-392
  branch, not the default branch.
  Evidence: `origin/feature/392-feature-request-live-ui-preview-for-ides` is at
  `21a3d17e8cde3d3d2c45afc527448ffbee22e792` and contains the live-preview work.

- Observation: the IR branch is not yet an ancestor of the branch-392
  integration base.
  Evidence: `git merge-base --is-ancestor origin/feature/422-create-ir-for-jniaot
  origin/feature/392-feature-request-live-ui-preview-for-ides` returned exit
  status 1.

## Decision Log

- Decision: use one master plan and ten sequential plans.
  Rationale: each execution remains bounded and resumable without loading the
  whole program into context.
  Date/Author: 2026-07-26 / OpenAI.

- Decision: perform artifact separation before physical source relocation.
  Rationale: consumers can be migrated without creating avoidable conflicts with
  the unfinished IR work.
  Date/Author: 2026-07-26 / OpenAI.

- Decision: keep the VS Code wizard and Maven-to-Gradle conversion in the
  extension.
  Rationale: they are user-facing workflows; shared tooling supplies mechanisms,
  not ownership of the interaction.
  Date/Author: 2026-07-26 / OpenAI.

- Decision: enforce 20 KiB and about 600 lines on every created or modified text
  file, except protected IR-related files.
  Rationale: smaller files improve maintenance and reduce agent context cost
  without destabilizing the IR branch.
  Date/Author: 2026-07-26 / User and OpenAI.

## Validation and Acceptance

Each numbered plan states focused acceptance. At every commit run `git diff
--check`, the staged file-size checker, and the smallest test that proves the
slice. Run broad SDK, plugin, platform, or packaging builds only at milestone
closure or when the changed contract requires them.

The complete program is accepted when a clean workspace can create or convert a
project in VS Code, start preview from CLI/Gradle/Maven/VS Code, rebuild after a
source edit, replace the worker, display the new frame, package through the typed
deploy path, and use side-by-side SDK/JDK/tool installations without relying on
SDK-bundled tooling resources.

## Risks and Open Questions

The exact source-history migration after the IR merge may reveal additional
cross-repository fixture coupling. Plan 09 must preserve history and may retain a
small compatibility module in `totalcross` if moving it would duplicate runtime
contracts. The plan must record that decision rather than forcing an unsafe move.

Vendor download endpoints and archive layouts may change. The store therefore
records concrete metadata and validates installed capabilities instead of
trusting a vendor name or a `latest` URL.

## Idempotence and Recovery

Every plan begins by verifying branch, origin, working-tree scope, previous
checkpoint, and state file. Existing directories are inspected, never
overwritten. Partial installations use staging directories and atomic rename.
Preview sessions use unique IDs and stale-session cleanup. A failed cross-repo
slice is recovered by reverting only its logical commits, never by resetting
unrelated work.

## Outcomes & Retrospective

Plan 01 completed the reproducible workspace bootstrap. The tooling repository
now owns the eleven canonical plans, checkpoint/evidence files, the staged
file-size checker, and a relative two-root VS Code workspace. The IR branch was
not merged: ancestry against the reviewed branch-392 integration base returned
exit status 1. Plan 02 is the next active slice; later outcomes remain pending.
Evidence is summarized in `.agent/evidence/unified-tooling-preview.md`.

## Revision Note

2026-07-26: created the sequential plan set, added bootstrap, branch-422 merge
protection, VS Code workflow preservation, token-efficient execution, and the
20 KiB/600-line file policy.

2026-07-26: bootstrap confirmed `origin/master` as the TotalCross integration
default branch, while the reviewed live-preview integration base is branch 392;
the IR merge gate against branch 392 remains unsatisfied.

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
