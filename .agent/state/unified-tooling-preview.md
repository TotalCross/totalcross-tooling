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
    resolver, model, VS Code input, and release details remained incomplete.
    Process-backed promotion is now implemented. The current decision gate is
    reproducible tooling-JDK installation before removing JavaJDKManager.

Next command:

    sed -n '1,300p'       .agent/plans/unified-tooling-preview/08c-pre-ir-architecture-corrections.md

Resume commands are repository-relative. Historical `/tmp` locations remain in
the append-only evidence file only.

## Repositories

Expected workspace:

    <workspace>/totalcross
    <workspace>/totalcross-tooling

TotalCross:

    branch:
      feature/392-feature-request-live-ui-preview-for-ides
    default branch:
      master

Tooling:

    branch:
      feature/unify-tooling-and-preview
    default branch:
      main

Fetch both remotes and record exact local and remote heads before the next
implementation commit. Do not copy stale SHAs from historical state entries.

## Completed checkpoints

Plans 01–08B recorded:

    bootstrap and workspace
    shared store and JDK provider model
    logical SDK artifacts and typed deploy
    Launcher decomposition and preview contract
    protocol, host, worker, and CLI modules
    Gradle and Maven adapters
    VS Code wizard, Maven conversion, and preview commands
    shared Android tool migration
    first complete installed-project E2E

Completed Plan 08C slices:

    reconciliation of state and audit evidence
    process-backed worker promotion through production CLI
    twenty real-worker replacements with failed-candidate preservation
    shared environment facade adopted by Gradle and Maven
    capability probes for selected tooling JDKs

Plan 08B remains evidence, not the final release gate.

## Active blockers

Current Plan 08C milestone:

    versioned immutable JDK catalog schema and parser
    bundled, file, and test catalog sources
    concrete JDK URLs and SHA-256 values
    Java 17 release matrix for macOS ARM64/x64, Linux x64, and Windows x64
    atomic shared-store JDK installation
    explicit jdkPath precedence with capability probes
    unsupported-platform jdkPath diagnostic
    dynamic providers restricted to catalog maintenance
    removal of Maven JavaJDKManager
    removal of latest URLs and forced arch=x86
    clean-cache installation and offline reuse

Remaining Plan 08C blockers:

    authoritative shared resolution across Gradle, Maven, CLI, and companion
    Java-17 plugin loading documentation and enforcement
    complete ProjectModel
    one public package path
    preview versus run semantics
    failed-build preservation in VS Code
    pointer scaling and Webview resize
    Maven Wrapper preference
    deterministic VSIX bundling
    removal of release-facing SNAPSHOT defaults

Plan 08D after Plan 08C:

    project-scoped Maven reminder suppression and reset
    shared Java legacy-conversion engine
    Gradle conversion task and CLI bootstrap conversion
    dynamic SDK and application-Java inference
    transactional apply, validation, and rollback
    installed-VSIX migration E2E

## JDK catalog decision

Use a versioned immutable catalog as the normal automatic-installation path.

Resolution order:

    explicit jdkPath
    permitted compatible JAVA_HOME
    verified existing shared-store installation
    bundled immutable catalog candidates
    future signed catalog updates, when implemented
    actionable failure requesting jdkPath

Every candidate passes the same capability probes. Dynamic vendor providers do
not install JDKs at runtime; they may only generate reviewed catalog candidates.

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
6. Do not remove JavaJDKManager until catalog equivalence tests pass.
7. Do not accept Plan 08C from catalog parser or coordinator tests alone.
8. Run clean-store, offline, and installed-VSIX E2E before Plan 08R.

## Deferrals and exclusions

Do not merge IR, move converter/deployer source, rewrite history, push, create
release branches, tag, publish, or delete caches unless separately authorized.
