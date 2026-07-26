<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Reconcile the IR branch and decide physical source ownership

This ExecPlan is Plan 09. It must not start until Plans 01 through 08 are complete.

## Purpose / Big Picture

Integrate the IR work safely, prove converter-generated fixtures still agree with
the native TCIR frontend, then move converter/deployer ownership only when the
validated dependency boundary supports it. Native TCIR, JIT, and AOT remain in
the TotalCross VM.

## Working Set and Resume Protocol

Read state, this plan, the IR branch state file, and only its active plan sections
for current paths and validation. Do not read its full history or evidence unless
a failure requires a prior command.

## Progress

- [ ] Fetch the reviewed branch-392 integration base and branch 422.
- [ ] Verify branch 422 is merged into the chosen integration base.
- [ ] Stop safely if the merge gate is not satisfied.
- [ ] Merge the integration base into branch 392 without rewriting history.
- [ ] Resolve conflicts with protected paths preserved.
- [ ] Revalidate converter fixtures, TCIR, JIT, AOT, and default-off behavior.
- [ ] Record the final converter-to-VM contract.
- [ ] Create verified backup refs before cross-repository import.
- [ ] Import agreed source history into tooling.
- [ ] Replace cross-repository fixture writes with a versioned fixture artifact.
- [ ] Keep compatibility facades and native backends in TotalCross.
- [ ] Commit paired checkpoints and update state to Plan 10.

## Current Architecture and Scope

The reviewed integration base for this program is the existing branch-392
remote branch, which already contains the live-preview implementation. The
merge gate is satisfied only when:

    git merge-base --is-ancestor       origin/feature/422-create-ir-for-jniaot \
      origin/feature/392-feature-request-live-ui-preview-for-ides

returns success. If it fails, update state with the observed refs and stop this
plan. Do not substitute `origin/master` or approximate the merge by
cherry-picking selected files.

Protected IR-related files may exceed the size limits and must not be split for
this program. If `TotalCrossVM/CMakeLists.txt` must be modified and is oversized,
extract focused build sections into small files under `TotalCrossVM/modules/`
before functional edits, because the root file is not protected.


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

Fetch refs and record exact SHAs. Verify both repositories have no unexplained
changes. Create local backup branches containing the pre-merge heads; do not push
them automatically.

After the gate is satisfied, continue from the local branch-392 checkout, which
is the integration branch for this program. Do not rebase or replace it with
`origin/master`. If a separate reviewed base is introduced later, merge that
base into branch 392 with a normal merge commit and document the chosen ref.
Resolve conflicts by preserving the branch-392 preview work and the merged IR
behavior. For protected paths, prefer the merged IR branch content unless branch
392 contains an independently required fix; document every manual combination.

Run the focused converter fixture generator and native TCIR differential tests
named by the merged IR plan. Verify default-off runtime behavior. Escalate to
platform builds only where the merged files are compiled or the IR plan requires
them.

Write a short local contract document describing the producer artifact: converter
version, bytecode/fixture schema version, deterministic inputs, generated fixture
format, and native consumer validation. Replace the current direct write from a
converter test into a sibling VM path with a generated fixture artifact. Prefer a
small neutral binary or text format plus metadata; the VM build converts it to a
header in its own build directory. Do not commit generated headers when they can
be reproduced.

For physical history migration, first use a temporary clone of `totalcross`.
Apply `git filter-repo` only to the temporary clone, never the working clone.
Select the agreed converter/deployer paths and preserve commit authors and dates.
Import the filtered history into a dedicated tooling integration branch, inspect
the graph, and merge without squashing. Move files into the final tooling module
paths with ordinary commits after the history import.

Before importing an oversized non-protected deployer file, split it by
responsibility in the source repository and validate the facade. Protected
converter files keep their current shape through the first move. Maintain thin
compatibility artifacts in TotalCross so `tc.Deploy` and legacy SDK classpaths
continue to work. Native `TotalCrossVM/src/tcvm/ir`, `jit`, and `aot` remain in
TotalCross.

If fixture coupling or licensing makes the move unsafe, record the evidence and
retain the converter source in TotalCross while keeping the artifact boundary.
That is an acceptable completed decision; do not force a move merely to satisfy
the original diagram.

## Surprises & Discoveries

- Observation: the live-preview branch is the reviewed integration base for this
  program; the default `origin/master` branch is not a substitute.
  Evidence: branch 392 is checked out at
  `21a3d17e8cde3d3d2c45afc527448ffbee22e792` and the user confirmed it already
  contains the live-preview changes.

- Observation: branch 422 is not yet an ancestor of the branch-392 base.
  Evidence: the branch-392 ancestry check returned exit status 1.

## Decision Log

- Decision: require ancestry proof before integration.
  Rationale: branch names alone do not prove the reviewed IR work is present.
  Date/Author: 2026-07-26 / OpenAI.

- Decision: use a temporary filtered clone for history import.
  Rationale: preserving source history must not rewrite either working repository.
  Date/Author: 2026-07-26 / OpenAI.

- Decision: allow a final decision to retain converter source in TotalCross.
  Rationale: validated runtime coupling takes precedence over a cosmetic repository
  boundary.
  Date/Author: 2026-07-26 / OpenAI.

## Validation and Acceptance

Acceptance first requires the merged branch to pass the converter fixture and
native IR focused suites with default compiled dispatch off. Then the chosen
ownership must be demonstrated:

If moved, tooling builds the converter/deployer artifacts from imported source,
TotalCross consumes the fixture artifact without sibling-path writes, and legacy
facades pass. If retained, the decision document identifies the concrete coupling
and all tooling consumers still use the narrow artifacts.

Inspect imported Git history for representative original authors and commits.
Run size-policy checks with protected warnings, `git diff --check`, and paired
repository tests. Do not push migration branches automatically.

## Risks and Open Questions

`git filter-repo` may be unavailable. Install it only with explicit local tooling
approval or use a temporary Python environment; never substitute a squashed copy
without recording the loss of history. License headers and repository licenses
must remain compatible; do not silently relicense LGPL source as Apache-2.0.

## Idempotence and Recovery

Backup refs and temporary clones make the import repeatable. If a filtered import
is wrong, delete only the temporary clone and unmerged tooling integration branch.
Do not reset merged working branches. Conflict resolution commits must remain
reviewable and path-scoped.

## Outcomes & Retrospective

Not started.

## Revision Note

2026-07-26: made branch ancestry a hard gate, preserved licensing and history,
and allowed evidence to reject an unsafe physical move.

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
