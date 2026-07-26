<!--
Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Unified tooling and preview evidence

This append-only file records concise validation results for the coordinated
program. Full command output belongs in `/tmp` or build artifacts.

## Plan 01 bootstrap

- 2026-07-26: all eleven numbered plan files were present and non-empty in the
  starting folder.
- 2026-07-26: TotalCross cloned at `21a3d17e8cde3d3d2c45afc527448ffbee22e792`
  on `feature/392-feature-request-live-ui-preview-for-ides`; tooling cloned at
  `caaa01b0b1d26c1112491d298f5b29a81be8d313` and branched as
  `feature/unify-tooling-and-preview`.
- 2026-07-26: TotalCross `origin/master` is the remote default at
  `b7c25d7762aa326bf0c3a9bd384c173efad006da`, but the reviewed integration base
  is branch 392 at `21a3d17e8cde3d3d2c45afc527448ffbee22e792`.
- 2026-07-26: IR branch `ff81ab91a3ca08045198855ddb26874bd20e7b9a` is not an
  ancestor of the branch-392 integration base. Full baseline output is in
  `/tmp/totalcross-bootstrap-baseline.log`.
- 2026-07-26: file-size-policy unit tests passed 5/5; staged `git diff --check`
  and checker validation passed before commits `3487465` and `c27a313`.
- 2026-07-26: local TotalCross branch 392 was rebased onto `origin/master`
  `b7c25d7762aa326bf0c3a9bd384c173efad006da` without conflicts. Resulting local
  HEAD is `d20214f87d8f936d851f3d77b37603625b838b99`; `git diff --check` passed,
  and no push was performed.
- 2026-07-26: Plan 02 inventory identified duplicated Gradle/Maven SDK and JDK
  resolution in the paths recorded in the plan's `Surprises & Discoveries`.
- 2026-07-26: `./tooling-java/gradlew -p tooling-java :tooling-core:test
  --console=plain` passed 8 tests; publication to Maven Local also passed.
  Implementation checkpoint: `e8488ef`.
- 2026-07-26: TotalCross `artifactContentTest` passed after confirming the
  versioned narrow JAR names; tooling core and Gradle plugin tests passed.
  Paired commits: TotalCross `0716e10af`, tooling `d2b646f`.
- 2026-07-26: Plan 04 focused launcher/parser/runtime/preview validation passed
  5 tests, including copied-frame ownership and neutral command lifecycle
  forwarding. Full output is in `/tmp/totalcross-plan04-preview-test.log` and
  `totalcross/TotalCrossSDK/agent-logs/20260726-190501-test-full.log`.
- 2026-07-26: Plan 05 tooling Java tests passed across protocol, host/worker,
  and existing tooling-core modules. The fat CLI JAR and install distribution
  also built; full outputs are `/tmp/tooling-plan05-test.log` and
  `/tmp/tooling-plan05-package.log`.
