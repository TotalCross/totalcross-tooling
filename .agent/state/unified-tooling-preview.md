<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Unified tooling and preview state

This is the concise resumable checkpoint for
`.agent/plans/unified-tooling-preview/`. Detailed history belongs in
`.agent/evidence/`, split by active plan when a journal reaches the text-file
size limit.

## Checkpoint

Active plan:

    Plan 08R — pre-IR release preparation

Reason:

    Plans 08C and 08D are complete. The next gate is a clean pre-IR release
    assembled from non-SNAPSHOT artifacts, with catalog-backed JDK installation,
    offline reuse, staged consumption, and the installed VSIX workflows proven.

Next command:

    sed -n '1,420p'       .agent/plans/unified-tooling-preview/08r-publish-pre-ir-release.md

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

Verified 2026-07-30 before Plan 08R preparation:

    totalcross-tooling local: 27e807848ed6ac3345730958d7c635a14fe0ef15
    totalcross-tooling origin/feature/unify-tooling-and-preview: 718eb450ca5e05099dafb1b59d0ccde5378642b2
    totalcross local and origin/feature/392-feature-request-live-ui-preview-for-ides: 9a36178ef185cc3986a446e7cdefdcb0451c402d

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
    versioned immutable JDK catalog with bundled, file, and test sources
    concrete Java 11 and 17 catalog entries for the minimum release host matrix
    atomic catalog installation, real macOS ARM64 probes, and offline reuse
    catalog resolver with probed jdkPath precedence and actionable fallback
    Gradle and Maven package/Retrolambda adapters migrated to the catalog
    Maven JavaJDKManager and its dynamic Zulu download test removed
    CLI worker launched with the catalog-selected Java 17 home
    Gradle and Maven preview coordinators launched with the catalog-selected
    Java 17 home and propagate that home to the CLI worker
    Java 17 loading requirement enforced by Gradle and declared by Maven
    versioned complete ProjectModel emitted by Gradle and Maven and consumed by CLI
    duplicate Gradle typed-package task removed; totalcrossPackage is the public path
    preview frame-stream and standalone run-window presentation modes separated
    VS Code preview commands prefer Maven Wrapper when present
    VS Code scales pointer input and applies configured device resize and density
    deterministic local VSIX packaging with runtime-content verification
    release-facing VS Code defaults use Gradle plugin 0.1.0 without Maven Local
    VS Code reload builds before control promotion and preserves the active frame on failure
    focused and installed-VSIX extension-host suites pass on VS Code 1.131.0
    preview protocol documentation distinguishes external integration from internal compatibility APIs
    Maven and Gradle SDK distribution resolution unified in tooling-core

Completed Plan 08D:

    project-scoped Maven reminder suppression and reset
    shared inventory, Java layout classification, and MainWindow evidence
    literal Unix/Windows script analysis with redaction
    versioned conversion plans with SDK and Java inference
    rendered Gradle project and official Wrapper assets
    fingerprint-checked transactional apply and journal rollback
    CLI analyze, apply, validate, and rollback bootstrap commands
    Gradle totalcrossConvertProject adapter using the shared engine
    deterministic companion JAR delivery and installed-VSIX conversion E2E

Plan 08B remains evidence, not the final release gate.

## Active blockers

Plans 08C and 08D gates pass. Plan 08R remains open for release assembly,
clean-room staging, catalog/offline verification, and explicit approval before
any public publication. The official VSIX runner was observed to block while
installing external Java Pack dependencies; the equivalent unpacked release
payload ran the installed-extension suite successfully.

Historical Plan 08D evidence:

    project-scoped Maven reminder suppression and reset (complete 2026-07-30)
    shared conversion module inventory and Java layout classification (complete 2026-07-30)
    schema-versioned dry-run conversion plans and CLI analyze command (complete 2026-07-30)
    literal Unix/Windows legacy-script evidence parser with secret redaction (complete 2026-07-30)
    script evidence included in versioned conversion-plan output (complete 2026-07-30)
    Java target inference from literal `javac --release` evidence and shared SDK ceiling (complete 2026-07-30)
    fingerprint-checked source-move transaction with rollback journal (complete 2026-07-30)
    CLI apply command with saved-plan drift verification and JSON result (complete 2026-07-30)
    CLI rollback command from persisted source-move journal (complete 2026-07-30)
    shared Gradle Wrapper validation and CLI validate command (complete 2026-07-30)
    script-backed SDK version candidates included in conversion plans (complete 2026-07-30)
    Gradle totalcrossConvertProject adapter delegates to the shared engine (complete 2026-07-30)
    deterministic VSIX companion JAR delivery with SHA-256 verification (complete 2026-07-30)
    VS Code conversion analysis command calls the packaged CLI only (complete 2026-07-30)
    shared renderer produces Gradle settings/build files from reviewed evidence (complete 2026-07-30)
    atomic generated-file transaction rejects collisions and rolls back owned files (complete 2026-07-30)
    CLI apply transacts rendered Gradle files and journal rollback removes them (complete 2026-07-30)
    conversion module bundles and writes official Gradle Wrapper assets (complete 2026-07-30)
    CLI apply and journal rollback transact Gradle Wrapper assets (complete 2026-07-30)
    VS Code explicit apply validates through CLI and rolls back validation failures (complete 2026-07-30)
    Gradle APPLY delegates to the same renderer and transaction as CLI (complete 2026-07-30)
    MainWindow classification follows unambiguous local inheritance (complete 2026-07-30)
    Java target inference accepts consistent javac source/target evidence and rejects conflicts (complete 2026-07-30)
    Java target inference falls back to compiled class-file majors after compiler flags (complete 2026-07-30)
    Java target inference reads literal existing Gradle/Maven build targets before classfiles (complete 2026-07-30)
    Launcher/Deploy plans separate recognized platforms, ordered arguments, and redacted secrets (complete 2026-07-30)
    shared Gradle renderer maps legacy deploy platforms and retained switches (complete 2026-07-30)
    literal root Gradle/Maven TotalCross SDK coordinates take precedence over scripts (complete 2026-07-30)
    project metadata supplies SDK evidence only after coordinates and legacy scripts (complete 2026-07-30)
    shared SDK catalog selects stable Maven metadata and verifies its offline cache (complete 2026-07-30)
    no-evidence conversion plans defer catalog SDK resolution until apply rendering (complete 2026-07-30)
    VS Code displays detailed reviewed conversion evidence, moves, generated files, and warnings (complete 2026-07-30)
    packaged VSIX verification and isolated installed-extension suite pass (complete 2026-07-30)
    conversion-plan JSON declares the exact conventional Gradle and Wrapper files to be generated (complete 2026-07-30)
    installed VSIX executes its bundled companion against a temporary legacy project (complete 2026-07-30)
    Gradle conversion task accepts reviewed MainWindow, SDK, and Java target selections (complete 2026-07-30)
    public CLI bootstrap analyze/apply/rollback works in a non-Gradle directory (complete 2026-07-30)
    installed VSIX companion E2E covers analyze, apply, wrapper validation, and rollback using a deterministic wrapper fixture (complete 2026-07-30)
    shared Java legacy-conversion engine (complete)
    Gradle conversion task and CLI bootstrap conversion (complete)
    dynamic SDK and application-Java inference (complete)
    transactional apply, validation, and rollback (complete)
    installed-VSIX migration E2E (complete)

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
