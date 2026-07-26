<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Bootstrap the coordinated TotalCross workspace

This ExecPlan follows the master plan and the repository rules that will be
loaded during bootstrap. It starts in a directory containing only the downloaded
plan set.

## Purpose / Big Picture

Create two verified sibling clones, select the correct branches, install the
plans in the tooling repository, create a multi-root VS Code workspace, and
record enough state that another model can resume without knowing the chat or
the original folder layout.

At completion, opening `totalcross-unified.code-workspace` shows both
repositories and the canonical state file identifies Plan 02 as the next action.

## Working Set and Resume Protocol

Before bootstrap, read this file from the download folder. After the plan commit,
stop updating the external copies and read:

    totalcross-tooling/.agent/state/unified-tooling-preview.md
    totalcross-tooling/.agent/plans/unified-tooling-preview/00-master-sequence.md
    totalcross-tooling/.agent/plans/unified-tooling-preview/02-shared-store-and-jdk.md

Use the state file as the first normal read. Store full command output in `/tmp`
and record only the result, commit, and log path in evidence.

## Progress

- [x] Verify the starting folder contains the complete numbered plan set.
- [x] Clone `TotalCross/totalcross` on branch 392.
- [x] Clone `TotalCross/totalcross-tooling` from remote `main`.
- [x] Create the tooling work branch.
- [x] Read repository `AGENTS.md` and `.agent/PLANS.md`.
- [x] Record initial commits and the IR-branch relationship.
- [x] Copy the plan set into the tooling repository.
- [x] Create state, evidence, archive, report, and size-policy checker.
- [x] Commit the canonical plans and support files.
- [x] Create and commit the multi-root workspace.
- [x] Remove only the now-redundant external plan copies.
- [x] Update state to Plan 02.

## Current Architecture and Scope

The starting directory must contain these files:

    00-master-sequence.md
    01-bootstrap-workspace.md
    02-shared-store-and-jdk.md
    03-artifact-and-deploy-boundaries.md
    04-preview-runtime-boundary.md
    05-preview-host-worker-cli.md
    06-gradle-and-maven-integration.md
    07-vscode-workflows.md
    08-hot-reload-and-tool-cache.md
    09-ir-merge-and-source-ownership.md
    10-sdk-slimming-and-final-validation.md

The canonical destination is:

    totalcross-tooling/.agent/plans/unified-tooling-preview/

The remote tooling base is `main`. The TotalCross remote default branch is
`master`; do not invent `origin/main` for that repository.
Create the local work branch `feature/unify-tooling-and-preview`.


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

Set `WORKSPACE_ROOT` to the physical starting directory and derive both clone
paths. Fail before any mutation if a target path exists but is not the expected
Git repository.

    export WORKSPACE_ROOT="$(pwd -P)"
    export TC_REPO="$WORKSPACE_ROOT/totalcross"
    export TC_TOOLING_REPO="$WORKSPACE_ROOT/totalcross-tooling"
    export TC_BRANCH="feature/392-feature-request-live-ui-preview-for-ides"
    export TOOLING_BASE_BRANCH="main"
    export TOOLING_WORK_BRANCH="feature/unify-tooling-and-preview"

Verify all eleven plan files exist and are non-empty. Clone TotalCross directly
on branch 392. Clone tooling from `main`, then create the work branch. Do not use
`--single-branch`; later plans need other refs.

    git clone --branch "$TC_BRANCH"       https://github.com/TotalCross/totalcross.git "$TC_REPO"

    git clone --branch "$TOOLING_BASE_BRANCH"       https://github.com/TotalCross/totalcross-tooling.git "$TC_TOOLING_REPO"

    git -C "$TC_TOOLING_REPO" switch -c "$TOOLING_WORK_BRANCH"

Verify branch, origin, and a clean scoped status. Read in full each existing
repository `AGENTS.md` and `.agent/PLANS.md`. If nested instructions apply to a
path changed by this plan, read them before editing.

Record these values in the canonical master and state file:

    git -C "$TC_REPO" rev-parse HEAD
    git -C "$TC_TOOLING_REPO" rev-parse HEAD
    git -C "$TC_REPO" remote get-url origin
    git -C "$TC_TOOLING_REPO" remote get-url origin
    git -C "$TC_REPO" branch --show-current
    git -C "$TC_TOOLING_REPO" branch --show-current

Fetch the IR branch and record whether it is already an ancestor of the reviewed
live-preview integration base `origin/feature/392-feature-request-live-ui-preview-for-ides`.
Do not merge it in this plan. `origin/master` is only the TotalCross remote
default branch and is not the base for this integration.

    git -C "$TC_REPO" fetch origin       feature/422-create-ir-for-jniaot main

    git -C "$TC_REPO" merge-base --is-ancestor       origin/feature/422-create-ir-for-jniaot origin/main

Copy all numbered plans into
`.agent/plans/unified-tooling-preview/`. Create the following small support files:

    .agent/state/unified-tooling-preview.md
    .agent/evidence/unified-tooling-preview.md
    .agent/archive/unified-tooling-preview-history.md
    .agent/reports/unified-tooling-preview-editorial.md

The state file is rewritten, not appended. It records active plan, active slice,
last logical commits in both repositories, baseline commits, active paths, next
command, focused validation, deferrals, blockers, and deliberate exclusions.
The evidence file is append-only and contains one compact record per meaningful
validation. Archive and report begin with a short explanation and no invented
results.

Create `scripts/check-file-size-policy.py`. It accepts a repository path and
checks staged added, copied, renamed, or modified text files. It fails when a
non-protected file exceeds 20,480 bytes or 600 lines. It skips binary files,
generated output, third-party/vendor directories, and deleted files. Protected
paths print a warning but do not fail. Add focused unit tests for path matching,
byte limit, line limit, binary detection, and protected exceptions. Keep both
script and tests below the policy limits.

Commit the plan set, support files, and checker in one isolated tooling commit:

    docs(plan): add unified tooling and preview plans

Create `totalcross-unified.code-workspace` in the tooling repository with two
folders: `../totalcross` named `TotalCross` and `.` named `TotalCross Tooling`.
Do not embed machine-specific absolute paths. Commit it separately:

    chore(workspace): add unified development workspace

Verify both commits contain only intended paths. Remove the external plan copies
only after `git ls-files` confirms every canonical plan exists. Do not remove the
workspace root or either clone.

## Surprises & Discoveries

- Observation: the TotalCross remote has no `origin/main` ref and its default
  branch is `origin/master`; the reviewed integration base is branch 392.
  Evidence: `git remote show origin` and `git ls-remote --heads origin` during
  bootstrap; the IR branch was compared with
  `origin/feature/392-feature-request-live-ui-preview-for-ides`.

- Observation: the IR branch is not an ancestor of `origin/master`.
  Evidence: the authoritative gate comparison against branch 392,
  `git merge-base --is-ancestor origin/feature/422-create-ir-for-jniaot
  origin/feature/392-feature-request-live-ui-preview-for-ides`, returned exit
  status 1.

## Decision Log

- Decision: make the tooling repository the canonical owner of all plans.
  Rationale: it remains the long-term home of shared tooling and avoids adding
  process files to the already active branch-392 work.
  Date/Author: 2026-07-26 / OpenAI.

- Decision: use a relative multi-root workspace stored in tooling.
  Rationale: sibling clones remain portable across user home directories.
  Date/Author: 2026-07-26 / OpenAI.

## Validation and Acceptance

Run focused checker tests, `git diff --check`, and the checker against its own
staged files before each commit. Acceptance requires:

    git -C "$TC_REPO" branch --show-current
    # feature/392-feature-request-live-ui-preview-for-ides

    git -C "$TC_TOOLING_REPO" branch --show-current
    # feature/unify-tooling-and-preview

    git -C "$TC_TOOLING_REPO" ls-files       .agent/plans/unified-tooling-preview/00-master-sequence.md

    python3 "$TC_TOOLING_REPO/scripts/check-file-size-policy.py"       --repo "$TC_TOOLING_REPO" --staged

Both repositories must have no unexplained changes. The state file must name
Plan 02 and its first concrete command.

## Risks and Open Questions

A pre-existing target directory may contain valuable work. Stop and report its
origin, branch, and scoped status; never overwrite or clean it. If the remote
branch names changed, record the actual remote refs and stop rather than guessing.

## Idempotence and Recovery

If a clone already exists, verify its origin and branch and reuse it only when
clean or when unrelated changes are clearly outside this plan. If the tooling
work branch exists, switch to it instead of recreating it. If canonical plans
are already committed, compare hashes and do not copy again. If the workspace
file already matches, do not rewrite it.

## Outcomes & Retrospective

Plan 01 completed on 2026-07-26. The two sibling repositories were cloned,
the expected branches were selected, and the tooling repository now owns all
eleven plans plus resumable state, append-only evidence, an archive, an
editorial report, and the staged file-size checker. The relative workspace is
committed separately. The only material deviation was the branch reference:
TotalCross uses `origin/master` as its remote default, while branch 392 is the
reviewed live-preview integration base and is the reference for the IR gate.

The checker tests passed 5/5, both staged validation runs passed, and no changes
were made to the TotalCross working tree. Plan 02 is the next executable slice.

## Revision Note

2026-07-26: created the bootstrap procedure and made plan canonicalization,
workspace creation, baseline capture, and size-policy enforcement explicit.

2026-07-26: recorded the actual TotalCross base as `origin/master` after the
remote rejected the planned `origin/main` ref.

2026-07-26: corrected the integration base to the existing branch-392 remote
branch, which already contains the live-preview changes; the IR gate is now
defined against that branch.

## Editorial Report

This section is mandatory at completion. Keep it factual and evidence-based.

### Editorial Summary

The program needed a reproducible two-repository workspace before shared
tooling and live-preview changes could be implemented safely. Plan 01 created
that workspace from the downloaded plan set and left a machine-independent VS
Code workspace plus a state file that identifies the next plan.

### Original Plan versus Actual Outcome

The planned bootstrap completed. The only correction was to distinguish the
TotalCross remote default branch (`origin/master`) from the reviewed branch-392
integration base, which already contains live-preview changes. The IR branch was
not merged because its ancestry gate against branch 392 is unsatisfied.

### What Changed

The tooling repository now contains the canonical plans under
`.agent/plans/unified-tooling-preview/`, checkpoint files under `.agent/`,
`scripts/check-file-size-policy.py`, its five-test unittest module, and
`totalcross-unified.code-workspace`. Two commits record these changes:
`3487465` for plans/support and `c27a313` for the workspace.

### Decisions and Trade-offs

Plans are canonical in tooling so the active TotalCross branch remains focused
on runtime work. The workspace uses sibling-relative paths for portability. The
size checker exempts protected IR paths but fails oversized ordinary staged text
files. The integration base is branch 392 rather than the remote default.

### Unexpected Problems and Discoveries

The planned `origin/main` ref does not exist in TotalCross. The remote default
is `origin/master`; subsequent integration checks use branch 392 because it is
the reviewed live-preview base.

### Validation and Measurable Results

`python3 -m unittest discover -s tests/file_size_policy -v` passed 5 tests.
`git diff --cached --check` and
`python3 scripts/check-file-size-policy.py --repo . --staged` passed before
both bootstrap commits.

### Useful Evidence and Examples

The baseline log is `/tmp/totalcross-bootstrap-baseline.log`. The resumable
checkpoint is `.agent/state/unified-tooling-preview.md`, and the relative
workspace is `totalcross-unified.code-workspace`.

### Limitations, Remaining Work, and Open Questions

No implementation plan beyond Plan 01 has started. The IR merge gate remains
open until branch 422 is an ancestor of branch 392. No pushes, pull requests,
tags, or releases were performed.

### Possible Article Angles

For maintainers, “Bootstrapping a resumable multi-repository implementation
plan” can explain why canonical state and evidence matter. For tooling authors,
“Separating an integration base from a repository’s default branch” can show how
branch provenance prevents an unsafe merge.

### Suggested Narrative

Start with the need to coordinate SDK/runtime and tooling repositories, describe
the branch and history constraints, show the canonical plan/state layout and
relative workspace, explain the missing `origin/main` discovery and correction
to branch 392, then close with the passing checker tests and the open IR gate.

### Claims Requiring Human Review

The statement that branch 392 contains the live-preview changes is based on the
user-provided integration direction and branch selection; review it against the
remote history before external publication. Commit hashes and test results are
locally verified.

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
