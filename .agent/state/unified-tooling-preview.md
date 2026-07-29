<!--
Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Unified tooling and preview state

This file is the resumable checkpoint for the sequential plans in
`.agent/plans/unified-tooling-preview/`. Read it before the active numbered plan.

## Checkpoint

Active plan: Plan 08B, pre-IR release stabilization.
Active slice: Plan 08 completed; shared external tools and Android deploy
migration are committed. The next slice consolidates the production preview
lifecycle and plugin/editor acceptance.
Next command: `cd /Users/flsobral/repos/totalcross-unified/totalcross-tooling &&
sed -n '1,300p' .agent/plans/unified-tooling-preview/08b-pre-ir-release-stabilization.md`

## Repositories

Workspace root: `/Users/flsobral/repos/totalcross-unified`

TotalCross repository: `totalcross`
Branch: `feature/392-feature-request-live-ui-preview-for-ides`
Remote branch baseline: `21a3d17e8cde3d3d2c45afc527448ffbee22e792`
Current rebased local commit: `0716e10af` (artifact boundary checkpoint)
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

TotalCross: rebase result `d20214f87d8f936d851f3d77b37603625b838b99`; artifact
boundary commit `0716e10af`; Plan 04 launcher/preview commit `e1d080e48`.
Tooling: bootstrap commit `3487465`; workspace commit `c27a313`; base correction
commit `1b7cc3e`; shared core commit `e8488ef`; typed deploy commit `d2b646f`;
Plan 04 docs commit `60d3726`; Plan 05 commit `026ed8a`; Plan 06 commit
`ce22dc9`; Plan 07 commit `a776258`; Plan 08 commit `27d3e18`.
TotalCross Plan 08 Android migration commit: `fac934fa3`.
Plan 08B stabilization slice: tooling `612e658`, frame/control continuation
`b9ece39`; TotalCross SDK `a020512e4`, headless-frame continuation
`fab77de18`, aggregate compatibility gate `6e9161739`.

## Active paths

`.agent/plans/unified-tooling-preview/`, `.agent/state/`, `.agent/evidence/`,
`.agent/archive/`, `.agent/reports/`, `scripts/check-file-size-policy.py`,
`tests/file_size_policy/test_check_file_size_policy.py`,
`totalcross-unified.code-workspace`, and `tooling-java/`.
Plan 03 also changed `TotalCrossSDK/build.gradle`, added
`TotalCrossSDK/gradle/artifact-boundaries.gradle`, its artifact contract test,
and the typed deploy proof paths in `gradle-plugin/`.

## Validation and evidence

Baseline command log: `/tmp/totalcross-bootstrap-baseline.log`.
Plan 01 validation: `python3 -m unittest discover -s tests/file_size_policy -v`
passed 5 tests; `git diff --cached --check` and the staged size-policy checker
passed before both commits.
Plan 02 validation: `./tooling-java/gradlew -p tooling-java
:tooling-core:test --console=plain` passed 8 tests, and
`:tooling-core:publishToMavenLocal` passed. Full logs are in
`/tmp/tooling-core-test.log` and `/tmp/tooling-core-publish.log`.
Plan 03 validation: SDK `artifactContentTest`, tooling core tests, and
`gradle-plugin/./gradlew test` passed. Logs are in
`/tmp/totalcross-artifact-boundaries.log`,
`/tmp/tooling-core-plan03-test.log`, and
`/tmp/gradle-plugin-plan03-test.log`.
Plan 04 validation: focused launcher/parser/runtime/preview tests passed 5
tests. Log is `/tmp/totalcross-plan04-preview-test.log`; the full SDK agent log
is `totalcross/TotalCrossSDK/agent-logs/20260726-190501-test-full.log`.
Plan 05 validation: tooling Java tests passed across all modules, including
protocol and host↔worker integration. Logs are `/tmp/tooling-plan05-test.log`
and `/tmp/tooling-plan05-package.log`.
Plan 06 validation: tooling-core and Gradle plugin tests passed, Maven package
passed, and the focused Maven manager test passed. Logs are
`/tmp/tooling-plan06-core-test.log`, `/tmp/gradle-plugin-plan06-test.log`, and
`/tmp/maven-plugin-plan06-package.log`.
Plan 07 validation: VS Code compile and the integration suite passed 21 tests;
full output is `/tmp/vscode-plan07-test.log`.
Plan 08 validation: tooling core passed 13 tests, the Gradle plugin suite passed,
the SDK compiled, and official Protobuf/Bundletool version probes passed. Logs:
`/tmp/tooling-plan08-core-test-final.log`,
`/tmp/gradle-plugin-plan08-test-final.log`,
`/tmp/totalcross-plan08-sdk-compile-final.log`, and
`/tmp/tooling-plan08-license.log`.
Plan 08B validation: tooling-java, Gradle, focused Maven/package, live-preview-
server, SDK compile, VS Code, license checks, and local staging passed. The
standalone CLI now produces a real fixture PNG (320x568) and the control-file
probe passed resize, pointer, key, and stop commands. VS Code now polls the
coordinator-owned frame and forwards those events; aggregate compatibility,
clean-cache staged consumption now resolves SDK, preview-runtime, and tooling
CLI artifacts without `mavenLocal` after staging the required annotations
artifact. Temporary Gradle and Maven projects now also pass compile, first-frame
preview, and stop through their native goals. The full Gradle/Maven/VS Code
matrix, installed VSIX acceptance, and public-baseline compatibility review
remain open. The logical stabilization commits include tooling `612e658`,
`b9ece39`, `8a2783f`, `3a1bc74` and TotalCross `a020512e4`, `fab77de18`,
`6e9161739`, `405275156`; the shared Java compatibility policy is tooling
`e080c4e`, and reload/session stabilization is tooling `0a0e04c`. The isolated
Gradle and Maven matrices now preserve the active worker through failed compile,
reload repaired classes, process controls/resources, and stop cleanly. Typed
deploy packaging, installed-VSIX activation, and public-baseline review remain
open.
The local VSIX was packaged/installed and the installed extension passed all 30
integration tests under the local VS Code executable; manual installed-project
E2E remains an explicit release gate.
An isolated published-Gradle-plugin packaging smoke resolved from staging and
reached `totalcrossPackage`, but was stopped because its synthetic SDK home did
not contain the complete distribution required by real `tc.Deploy`; the typed
contract is covered by the plugin functional suite, while release-grade real
packaging remains open.
The legacy deploy adapter was hardened in tooling `e8c79a4` to isolate
non-daemon deploy threads and honor the requested SDK home. Core/tooling and
plugin tests passed; a real SDK packaging smoke remains open after an external
`Connection reset`.
Failure output is now retained by tooling `d0c6e1e`; tooling-core validation
passed, but the real packaging smoke has not been rerun to completion.
Preview version gating is proven against cached SDK 7.2.0: the CLI returns a
structured compatibility error and exit 1 when the required preview contract is
absent. Aggregate binary compatibility with that public baseline remains open.
The aggregate check records concrete removals/modifications against 7.2.0;
public publication therefore remains blocked pending an explicit versioning or
compatibility decision. The current preview-capable floor proven locally is
SDK 7.2.2.

## Deferrals and exclusions

Do not merge or modify the IR branch in Plan 01. Do not move converter or
deployer sources. Do not push, open pull requests, tag, release, rewrite
history, or remove either repository. Generated files, caches, and build output
remain excluded.

## Blockers

The IR merge gate is intentionally unsatisfied and is a Plan 09 prerequisite.
Use branch 392 as the reviewed integration base when Plan 09 evaluates ancestry;
`origin/master` is only the remote default branch. Plan 08B must complete before
release branches or publication are considered.
