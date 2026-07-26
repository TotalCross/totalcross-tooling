<!--
Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Unified tooling and preview state

This file is the resumable checkpoint for the sequential plans in
`.agent/plans/unified-tooling-preview/`. Read it before the active numbered plan.

## Checkpoint

Active plan: Plan 03, artifact and deploy boundaries.
Active slice: Plan 02 completed; inspect the state and Plan 03 before
inventorying SDK artifact ownership and deploy invocations.
Next command: `cd /Users/flsobral/repos/totalcross-unified/totalcross-tooling &&
sed -n '1,220p' .agent/plans/unified-tooling-preview/03-artifact-and-deploy-boundaries.md`

## Repositories

Workspace root: `/Users/flsobral/repos/totalcross-unified`

TotalCross repository: `totalcross`
Branch: `feature/392-feature-request-live-ui-preview-for-ides`
Remote branch baseline: `21a3d17e8cde3d3d2c45afc527448ffbee22e792`
Current rebased local commit: `d20214f87d8f936d851f3d77b37603625b838b99`
Origin: `https://github.com/TotalCross/totalcross.git`

Tooling repository: `totalcross-tooling`
Branch: `feature/unify-tooling-and-preview`
Baseline commit: `caaa01b0b1d26c1112491d298f5b29a81be8d313`
Origin: `https://github.com/TotalCross/totalcross-tooling.git`

## IR merge gate

Fetched IR branch commit: `ff81ab91a3ca08045198855ddb26874bd20e7b9a`.
The TotalCross remote default branch is `master` (`origin/HEAD` points to it),
not `main`; its commit is `b7c25d7762aa326bf0c3a9bd384c173efad006da`.
The reviewed live-preview integration base is
`origin/feature/392-feature-request-live-ui-preview-for-ides` at
`21a3d17e8cde3d3d2c45afc527448ffbee22e792`. The IR branch is not an ancestor
of that base. No merge was attempted.
The local branch was explicitly rebased onto `origin/master` at
`b7c25d7762aa326bf0c3a9bd384c173efad006da`; the rebase completed without
conflicts and `git diff --check` passed. The remote branch was not pushed.

## Logical commits

TotalCross: no commits created by this program.
Tooling: bootstrap commit `3487465`; workspace commit `c27a313`; base correction
commit `1b7cc3e`; shared core commit `e8488ef`.

## Active paths

`.agent/plans/unified-tooling-preview/`, `.agent/state/`, `.agent/evidence/`,
`.agent/archive/`, `.agent/reports/`, `scripts/check-file-size-policy.py`,
`tests/file_size_policy/test_check_file_size_policy.py`,
`totalcross-unified.code-workspace`, and `tooling-java/`.

## Validation and evidence

Baseline command log: `/tmp/totalcross-bootstrap-baseline.log`.
Plan 01 validation: `python3 -m unittest discover -s tests/file_size_policy -v`
passed 5 tests; `git diff --cached --check` and the staged size-policy checker
passed before both commits.
Plan 02 validation: `./tooling-java/gradlew -p tooling-java
:tooling-core:test --console=plain` passed 8 tests, and
`:tooling-core:publishToMavenLocal` passed. Full logs are in
`/tmp/tooling-core-test.log` and `/tmp/tooling-core-publish.log`.

## Deferrals and exclusions

Do not merge or modify the IR branch in Plan 01. Do not move converter or
deployer sources. Do not push, open pull requests, tag, release, rewrite
history, or remove either repository. Generated files, caches, and build output
remain excluded.

## Blockers

The IR merge gate is intentionally unsatisfied and is a Plan 09 prerequisite.
Use branch 392 as the reviewed integration base when Plan 09 evaluates ancestry;
`origin/master` is only the remote default branch.
