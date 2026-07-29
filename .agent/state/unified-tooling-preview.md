<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Unified tooling and preview state

This is the concise resumable checkpoint for
`.agent/plans/unified-tooling-preview/`. Detailed history belongs in
`.agent/evidence/unified-tooling-preview.md`.

## Checkpoint

Active plan:

    Plan 08C — correct pre-IR preview and plugin architecture

Reason:

    Plan 08B produced a useful E2E checkpoint, but a later audit found that
    production reload reused one worker and that resolver, model, VS Code input,
    and release details remained incomplete.

Next command:

    sed -n '1,260p'       .agent/plans/unified-tooling-preview/08c-pre-ir-architecture-corrections.md

Resume commands are repository-relative. Historical `/tmp` locations remain in
the append-only evidence file only, where they identify prior validation logs.

## Repositories

Expected workspace:

    <workspace>/totalcross
    <workspace>/totalcross-tooling

TotalCross:

    branch:
      feature/392-feature-request-live-ui-preview-for-ides
    local head verified on 2026-07-29:
      9a36178ef185cc3986a446e7cdefdcb0451c402d
    remote head verified on 2026-07-29:
      9a36178ef185cc3986a446e7cdefdcb0451c402d
    default branch:
      master

Tooling:

    branch:
      feature/unify-tooling-and-preview
    local head verified on 2026-07-29:
      1ac618317681fb9b93add4aeb02e5dfd5c51668d
    remote head verified on 2026-07-29:
      7c2baaa6ef3cd6ecb06e1510514ae5741c183881
    default branch:
      main

Both remotes were fetched with pruning on 2026-07-29 before Plan 08C work.
Tooling is one local documentation commit ahead of its feature remote; TotalCross
is aligned with its feature remote. No remote write was performed.

## Completed checkpoints

Plans 01–08B recorded:

    bootstrap and workspace
    shared store and JDK providers
    logical SDK artifacts and typed deploy
    Launcher decomposition and preview contract
    protocol, host, worker, and CLI modules
    Gradle and Maven adapters
    VS Code wizard, Maven conversion, and preview commands
    shared Android tool migration
    first complete installed-project E2E

Plan 08B remains evidence, not the final release gate.

## Active blockers

Plan 08C:

    failed-build preservation
    authoritative shared environment resolution
    complete project model
    one public package path
    preview versus run semantics
    pointer scaling and Webview resize
    Maven Wrapper preference
    deterministic VSIX bundling
    removal of release-facing SNAPSHOT defaults

Completed Plan 08C milestones:

    reconciliation of remote heads, resume commands, and audit evidence
    process-backed worker promotion through the production CLI
    twenty real-worker replacements with failed-candidate preservation
    shared environment facade adopted by Gradle and Maven with JDK probes

Next milestone: materialize JDKs through the shared store, then remove Maven's
Zulu-only downloader after equivalence tests.

Plan 08D after 08C:

    project-scoped Maven reminder suppression and reset
    shared Java legacy-conversion engine
    Gradle conversion task for existing builds
    CLI bootstrap conversion for non-Gradle folders
    dynamic SDK and Java inference
    transactional apply, validation, and rollback
    installed-VSIX migration E2E

## IR merge gate

Previously recorded IR checkpoint:

    feature/422-create-ir-for-jniaot
    ff81ab91a3ca08045198855ddb26874bd20e7b9a

Verify its current head during Plan 09. Do not merge or modify it in Plans 08C,
08D, or 08R.

## Release status

No release branch, public publication, public tag, or IR merge is authorized by
this state. Plan 08R starts only after Plans 08C and 08D complete.

## Resume rules

1. Verify repository, branch, origin, head, and working-tree scope.
2. Read only the active plan and current milestone paths.
3. Run focused tests and save verbose logs outside plan files.
4. Commit one accepted milestone at a time.
5. Update this state after each milestone.
6. Do not accept Plan 08C from coordinator unit tests alone.
7. Do not accept Plan 08D from TypeScript-only tests.
8. Run installed bundled VSIX E2E before Plan 08R.

## Deferrals and exclusions

Do not merge IR, move converter/deployer source, rewrite history, push, create
release branches, tag, publish, or delete caches unless separately authorized.
