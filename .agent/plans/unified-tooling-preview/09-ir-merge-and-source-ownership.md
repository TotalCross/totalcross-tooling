<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Reconcile the IR branch and decide physical source ownership

This ExecPlan is Plan 09. Start only after Plan 08R records a verified pre-IR
release and its exact release commits and tags.

## Purpose / Big Picture

Integrate IR work on top of a known published preview/tooling baseline, revalidate
converter/native contracts, and decide physical source ownership without
destabilizing the released line. Native TCIR, JIT, and AOT remain in TotalCross.

## Working Set and Resume Protocol

Read state, this plan, the pre-IR release manifest, and only active IR plan
sections. Do not reread full release evidence unless a regression requires it.

## Progress

- [ ] Verify the pre-IR release is publicly or explicitly internally verified.
- [ ] Record release branches, commits, tags, and compatibility matrix.
- [ ] Fetch branch 392, branch 422, and the reviewed integration base.
- [ ] Verify the chosen integration strategy and exact refs.
- [ ] Create local backup refs.
- [ ] Merge IR with normal history-preserving commits.
- [ ] Resolve conflicts while preserving preview/tooling contracts.
- [ ] Revalidate converter fixtures, TCIR, JIT, AOT, and default-off behavior.
- [ ] Re-run focused pre-IR preview/package compatibility tests.
- [ ] Record the final converter-to-VM contract.
- [ ] Decide whether physical converter/deployer movement is safe.
- [ ] If approved, import history through a temporary filtered clone.
- [ ] Replace sibling-path fixture writes with a versioned fixture artifact.
- [ ] Keep compatibility facades and native backends in TotalCross.
- [ ] Commit paired checkpoints and update state to Plan 10.

## Current Architecture and Scope

The pre-IR release branches are frozen and are not merge targets for IR. Continue
IR work on the feature branches or a new integration branch derived from the
accepted branch-392 feature commit.

Do not require branch 422 to be an ancestor of branch 392 before integration; the
purpose of this plan is to merge the reviewed IR ref. Instead verify:

    exact branch-392 head
    exact branch-422 head
    common base
    pre-IR release commit
    intended merge direction

Use a normal merge. Do not rebase the long-running reviewed branches or
cherry-pick an incomplete subset.


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

Create local backup refs for both feature heads. Merge branch 422 into the
branch-392 integration branch or a named integration branch created from it.
Document the exact command and merge base.

Resolve protected-path conflicts by preserving reviewed IR behavior and applying
only independently required preview/tooling compatibility changes. Document each
manual combination. Native IR/JIT/AOT paths remain in TotalCross.

Run the converter fixture generator and native TCIR differential tests required
by the IR work. Verify default-off behavior and any opt-in dispatch modes.
Re-run focused aggregate SDK, Launcher, typed deploy, CLI preview, Gradle preview,
Maven preview, and VS Code session tests to prove the merge did not regress the
published contract.

Write a concise converter-to-VM contract: producer version, schema, deterministic
inputs, generated fixture format, and native validation. Replace direct writes
from converter tests into sibling VM source paths with a versioned fixture
artifact or generated build input. Do not commit reproducible generated headers.

Physical movement is a separate decision after semantic validation. If approved,
filter history only in a temporary clone, import it into a dedicated tooling
branch, inspect authors and commits, and merge without squashing. Preserve
licenses. Protected converter files keep their current shape during the first
move. Split oversized non-protected deployer files before movement.

If coupling, licensing, or release maintenance makes movement unsafe, retain
source in TotalCross and keep the artifact boundary. That is an acceptable
outcome.

## Decision Log

- Decision: release first, then merge IR on development branches.
  Rationale: the pre-IR release is a stable rollback and support line.
  Date/Author: 2026-07-28 / User and OpenAI.

- Decision: use a reviewed merge, not ancestry as a prerequisite.
  Rationale: branch 422 is expected to be integrated by this plan rather than
  already present in branch 392.
  Date/Author: 2026-07-28 / OpenAI.

- Decision: physical movement remains evidence-driven.
  Rationale: runtime and fixture coupling may justify retaining source ownership.
  Date/Author: 2026-07-28 / OpenAI.

## Validation and Acceptance

Acceptance requires IR suites and focused pre-IR release compatibility tests to
pass on the merged development branch. The published release branches remain
unchanged.

If source moves, tooling builds the imported artifacts, fixture exchange no
longer writes across repositories, history and licenses are preserved, and
legacy facades pass. If source remains, record the concrete reason and prove all
consumers still use narrow artifacts.

Run size checks with protected warnings, `git diff --check`, and paired repository
tests. Do not push migration or release changes without explicit approval.

## Risks and Open Questions

IR may change assumptions used by converter artifact boundaries. Prefer adapters
over rewriting released public APIs. `git filter-repo` availability and licensing
must be handled explicitly.

## Idempotence and Recovery

Release tags and branches are untouched. Backup refs and temporary clones make
integration repeatable. A failed history import is discarded without resetting
the working repositories.

## Outcomes & Retrospective

Not started.

## Revision Note

2026-07-28: made a verified pre-IR release the prerequisite, corrected the merge
gate to perform the IR merge, and protected the released support line.

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
