<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Add shared legacy-project conversion and reminder suppression

This ExecPlan is Plan 08D. Start only after Plan 08C acceptance. It adds migration
capabilities without moving analysis or mutation logic into the VS Code
extension.

## Purpose / Big Picture

Allow a developer to select an arbitrary local Java/TotalCross folder and obtain
a standard validated Gradle Java project. The shared Java conversion engine
detects layout and legacy build evidence, produces a dry-run plan, applies a
transaction with rollback, and validates the result.

The Gradle plugin exposes the engine as a task when the folder already has a
Gradle build. The tooling CLI exposes the same engine for folders that are not
yet Gradle projects. VS Code is a thin consumer: it selects the folder, displays
the structured plan, requests apply/cancel, and presents diagnostics.

The extension also allows a user to suppress the automatic Maven-to-Gradle
reminder for one project and later re-enable it.

## Working Set and Resume Protocol

Read state and this plan. Inspect only:

    tooling-java settings and the new project-conversion module
    tooling CLI command registration and JSON event support
    Gradle plugin task registration and conversion extension
    VS Code reminder, commands, companion client, and tests
    existing Maven-to-Gradle transaction engine and Gradle templates
    shared SDK version catalog and JavaCompatibilityPolicy

Do not duplicate conversion rules in TypeScript. Do not execute discovered shell
or batch scripts. Do not merge IR, create release branches, or publish.

## Progress

### Shared engine

- [ ] Add a small `tooling-project-conversion` Java module.
- [ ] Implement streaming inventory and ignored-path rules.
- [ ] Infer main sources, tests, resources, and package paths.
- [ ] Locate MainWindow candidates and report ambiguity.
- [ ] Parse supported Unix and Windows legacy command evidence.
- [ ] Infer SDK, Java target, Launcher arguments, and Deploy arguments.
- [ ] Resolve missing SDK dynamically from the shared stable catalog.
- [ ] Resolve missing Java from highest target accepted by that SDK.
- [ ] Generate a versioned conversion plan and diagnostic report.
- [ ] Apply a hash-verified transaction with backup and rollback.
- [ ] Validate the resulting Gradle project through its wrapper.

### Gradle and CLI entry points

- [ ] Add `totalcrossConvertProject` to the Gradle plugin.
- [ ] Make the task use only the shared conversion engine.
- [ ] Add CLI `convert-project analyze`, `apply`, `validate`, and `rollback`.
- [ ] Emit versioned JSON lines suitable for IDE consumption.
- [ ] Keep CLI as the bootstrap path when no Gradle build exists.

### VS Code consumer

- [ ] Add “Don't Ask Again for This Project” to the Maven reminder.
- [ ] Store suppression per workspace folder without changing project files.
- [ ] Add `TotalCross: Enable Maven Conversion Reminder`.
- [ ] Add `TotalCross: Convert to TotalCross Project`.
- [ ] Call the shared CLI; do not classify or move files in TypeScript.
- [ ] Display detected evidence, conflicts, moves, generated files, and warnings.
- [ ] Require explicit Apply before mutation.
- [ ] Present rollback and validation results.
- [ ] Pass unit, fixture, integration, and installed-VSIX E2E tests.
- [ ] Commit and update state to Plan 08R.


## Cross-plan safety and size policy

Run one plan at a time and preserve unrelated work. Never use `git reset --hard`,
`git clean -fd`, force-push, history rewriting, tag deletion, or repository
archival unless the user explicitly requests that exact operation.

Every implementation text file created or modified in the TotalCross repositories
must remain at or below 20 KiB and approximately 600 lines. Run the staged
size-policy checker before every commit. Split an oversized non-protected file by
responsibility before changing its behavior. Do not split a protected IR file
merely to satisfy this rule.

Protected paths:

    TotalCrossSDK/src/main/java/tc/tools/converter/**
    TotalCrossSDK/src/test/java/tc/tools/converter/**
    TotalCrossVM/src/tcvm/ir/**
    TotalCrossVM/src/tcvm/jit/**
    TotalCrossVM/src/tcvm/aot/**
    TotalCrossVM/src/tests/ir/**
    docs/architecture/bytecode/**

A project-conversion operation may relocate a pre-existing user file without
rewriting or splitting its contents, even when that file is larger. Newly
generated build files, reports, journals, and implementation files remain
subject to the limit. Split a large migration journal into numbered chunks.

Generated build output, caches, downloaded tools, credentials, and third-party
content must not be committed.

Use token-efficient execution. Read the state file and active plan first. Inspect
only named paths. Save verbose logs under `/tmp` or build artifacts and record
only concise outcomes, commit IDs, and log paths. Do not repeatedly print full
plans, diffs, generated projects, dependency trees, or test logs.

## Architecture

Create:

    tooling-java/
      tooling-project-conversion/

Suggested packages:

    com.totalcross.tooling.conversion.inventory
    com.totalcross.tooling.conversion.java
    com.totalcross.tooling.conversion.script
    com.totalcross.tooling.conversion.inference
    com.totalcross.tooling.conversion.plan
    com.totalcross.tooling.conversion.transaction
    com.totalcross.tooling.conversion.validation

The module may depend on `tooling-core` for SDK catalog, Java compatibility,
process execution, store layout, and diagnostics. It must not depend on Gradle,
Maven, VS Code, AWT, or TotalCross SDK classes.

The Gradle plugin depends on the module and registers:

    totalcrossConvertProject

The task supports:

    mode = ANALYZE | APPLY | VALIDATE | ROLLBACK
    projectDirectory
    planFile
    reportDirectory
    selectedMainWindow
    selectedSdkVersion
    selectedJavaTarget
    nonInteractive

For a folder without a Gradle build, VS Code calls:

    totalcross-tooling convert-project analyze --project <path>
    totalcross-tooling convert-project apply --plan <file>
    totalcross-tooling convert-project validate --project <path>
    totalcross-tooling convert-project rollback --journal <file>

Do not generate a temporary Gradle init script merely to load the plugin into a
non-Gradle folder. The CLI and Gradle task must call the same engine and produce
the same plan schema.

## Plan of Work

### Milestone 1: Maven reminder preference

For a Maven-only TotalCross project, show:

    Convert to Gradle
    Later
    Don't Ask Again for This Project

Store suppression in `ExtensionContext.workspaceState`, keyed by normalized
workspace-folder URI and a stable project identity. Do not write project settings.
In multi-root workspaces, suppress only the selected folder.

The manual Maven conversion command remains available. Add:

    TotalCross: Enable Maven Conversion Reminder

It clears suppression only for the selected project. Add tests for reload,
multi-root isolation, explicit reset, and a folder whose path changes.

### Milestone 2: inventory and classification

Select one project root. Never follow a symlink outside it. Ignore:

    .git
    .gradle
    build
    target
    out
    bin
    node_modules
    caches
    generated deploy/package output
    migration backup directories

Inventory path, type, size, and hash without loading all files into memory.

Recognize standard Gradle roots first. For other Java files, parse package
declarations and classify conservatively:

    test:
      test/tests path, test-style name, or supported test annotation/import

    main:
      other Java sources

    resource:
      non-Java files under resource-like roots, or detected classpath resources

Map Java files to package-consistent paths below `src/main/java` or
`src/test/java`. Preserve default-package sources and warn. Preserve relative
resource paths. Ambiguous classification requires user selection.

### Milestone 3: MainWindow and legacy script evidence

Parse Java declarations and inheritance. Prefer a concrete public class extending
`totalcross.ui.MainWindow`, directly or through an unambiguous local superclass.
A class named `MainWindow` is only a weaker candidate.

One candidate is selected. Multiple candidates are returned with evidence. No
candidate stops before mutation.

Read, but never execute:

    *.sh
    *.bash
    *.command
    *.bat
    *.cmd
    Makefile and makefile when present

Recognize literal commands and simple same-file variable assignments. Do not
evaluate command substitution, sourced files, macros, or arbitrary shell.

Extract file-and-line evidence for:

    javac --release
    javac -source and -target
    jar creation and main-class options
    TotalCross coordinates, archive names, paths, and versioned URLs
    java ... totalcross.Launcher plus ordered arguments
    java ... tc.Deploy plus ordered arguments
    deploy platforms, application name, output, resources, and certificates
    activation-key presence without recording its value

Redact secret values. Never copy an activation key into generated files.

### Milestone 4: SDK and Java inference

SDK precedence:

1. existing Gradle or Maven TotalCross coordinate;
2. unambiguous script or SDK archive version;
3. unambiguous project metadata;
4. latest stable version from the shared SDK catalog.

Resolve latest at execution time. Use verified cached metadata offline. If no
online or verified cached catalog can identify a version, stop and ask.

Java target precedence:

1. `javac --release`;
2. consistent `-source` and `-target`;
3. existing build configuration;
4. classfile major version from compiled output;
5. `JavaCompatibilityPolicy.highestApplicationTarget(sdkVersion)`.

Conflicting evidence is returned for selection. The tooling JDK is not the
application target.

### Milestone 5: conversion plan and generated project

Produce a versioned plan containing:

    project root and inventory fingerprint
    detected source, test, and resource roots
    proposed source and resource moves
    MainWindow candidates and selection
    SDK and Java evidence plus selected values
    generated and modified files
    conflicts and hash-identical no-op moves
    Launcher and Deploy argument mapping
    redacted secret warnings
    validation commands
    backup and journal locations

Generate or update:

    settings.gradle or settings.gradle.kts
    build.gradle or build.gradle.kts
    Gradle Wrapper
    src/main/java
    src/test/java
    src/main/resources
    src/test/resources when needed

Prefer the official template. Do not overwrite unrelated Gradle files. If the
project is already conventional Gradle, add only missing TotalCross configuration.

The plugin model must support:

    mainClass
    sdkVersion
    application Java target
    launcherArguments
    deployArguments
    platforms
    application name
    external resources

Map recognized non-secret arguments to typed properties. Preserve unknown ordered
switches. Report secret-bearing options separately for local Gradle property or
environment configuration.

If the selected folder is Maven, reuse the existing Maven-to-Gradle transaction
engine where its operations match this plan.

### Milestone 6: transaction and rollback

No mutation occurs during analyze. VS Code displays the plan and asks Apply or
Cancel.

Before apply, verify the inventory fingerprint. Create a unique backup and
chunked operation journal. Reject destination collisions unless source and
destination hashes match.

Prefer atomic rename on the same filesystem. Otherwise copy, verify hash, then
remove the source. On ordinary failure, restore moved and overwritten files and
remove only files created by this transaction. Preserve the failed journal and
diagnostic report.

Re-running against an already converted project reports its state and does not
duplicate files.

### Milestone 7: validation

Run the generated wrapper with `shell: false`:

    classes
    testClasses when tests exist
    totalcrossProjectModel
    totalcrossPreview with no launch, or an equivalent compatibility check

Use configured/staged repositories, not `mavenLocal`. Validation failure rolls
back by default unless the user explicitly chooses to keep the generated state.

### Milestone 8: tests

Fixtures:

    conventional Gradle project
    Maven TotalCross project
    flat Java project
    separate source/test/resource roots
    Unix script with javac, Launcher, and Deploy
    Windows cmd with quoted paths and arguments
    missing SDK version
    missing Java version
    conflicting versions
    multiple MainWindow candidates
    default-package source
    destination collision
    secret-bearing deploy command
    symlink outside the project
    forced Gradle validation failure

Acceptance requires:

1. Gradle task and CLI produce identical normalized plans;
2. VS Code performs no classification or filesystem mutation itself;
3. reminder suppression is project-scoped and resettable;
4. Unix and Windows evidence is detected without script execution;
5. missing SDK uses dynamic latest stable catalog metadata;
6. missing Java uses the highest target accepted by that SDK;
7. ambiguous evidence requires selection;
8. rollback restores original hashes and paths;
9. installed bundled VSIX passes analyze, apply, validation, and rollback E2E.

Run focused tests after each milestone, then full relevant suites, license checks,
`git diff --check`, and staged size checks.

## Decision Log

- Decision: shared Java tooling owns conversion.
  Rationale: Gradle and CLI can reuse one safe engine; VS Code remains a consumer.
  Date/Author: 2026-07-29 / User and OpenAI.

- Decision: CLI bootstraps non-Gradle folders.
  Rationale: a folder without a Gradle build cannot normally apply the plugin.
  Date/Author: 2026-07-29 / OpenAI.

- Decision: scripts are evidence only.
  Rationale: executing arbitrary legacy build scripts is unsafe.
  Date/Author: 2026-07-29 / User and OpenAI.

- Decision: ambiguity requires selection.
  Rationale: conservative interruption is preferable to destructive guesses.
  Date/Author: 2026-07-29 / OpenAI.

## Validation and Acceptance

Plan 08D completes only when every progress and Milestone-8 item passes and the
installed VSIX invokes the packaged shared CLI rather than source checkout or
TypeScript migration logic.

## Risks and Open Questions

Shell parsing is intentionally incomplete. Unsupported syntax is reported as
unresolved evidence. Large projects require streamed inventory and chunked
journals.

## Idempotence and Recovery

Analysis is read-only. Apply verifies the saved fingerprint. Transactions are
hash-verified and reversible. Plan 08R remains blocked until installed-VSIX
conversion passes.

## Outcomes & Retrospective

Not started.

## Revision Note

2026-07-29: created the shared Gradle/CLI conversion engine plan and project-scoped
Maven reminder preference.


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
