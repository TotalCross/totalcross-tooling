<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Integrate Gradle and Maven with the shared tooling

This ExecPlan is Plan 06. It gives both build systems equivalent compile, run,
package, and preview behavior while keeping their native task and goal models.

## Purpose / Big Picture

A Gradle or Maven project can start TotalCross preview through its own build
command. Both plugins use the same SDK/JDK selection, Java target policy,
Retrolambda decision, deploy service, and session descriptor. Source edits
compile before the worker is replaced.

## Working Set and Resume Protocol

Read state and this plan. Inspect only current Gradle/Maven plugin build model,
SDK/JDK resolver, Retrolambda, package, run, and preview-related paths. Avoid
unrelated cross-plugin refactors.

## Progress

- [ ] Define the shared project and preview session model.
- [ ] Migrate Gradle SDK/JDK and Java compatibility logic to shared tooling.
- [ ] Add Gradle run, preview, preview-stop, and session tasks.
- [ ] Migrate Maven SDK/JDK and Java compatibility logic to shared tooling.
- [ ] Add Maven run and preview goals.
- [ ] Add source/resource tracking and debounce.
- [ ] Add equivalent functional projects and tests.
- [ ] Commit and update state to Plan 07.

## Current Architecture and Scope

Add shared immutable types, in a small `tooling-build-model` module when needed:

    ProjectModel
    BuildTool
    SourceRoot
    ResourceRoot
    ClassOutput
    DependencyClasspath
    JavaCompatibilityPolicy
    RetrolambdaPlan
    PreviewSessionDescriptor
    BuildNotification

The model is serialized as versioned JSON for the CLI and VS Code. It uses
absolute normalized runtime paths in the generated session file but never commits
those files.


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

In the Gradle plugin, remove duplicated resolver behavior only after shared-core
tests cover it. Keep Gradle-specific providers, properties, task wiring, and
incremental annotations in the plugin. Add:

    totalcrossProjectModel
    totalcrossRun
    totalcrossPreview
    totalcrossPreviewStop

`totalcrossPreview` depends on compiled classes and writes a session descriptor.
It starts the external host only when no compatible live host exists, sends a
reload notification, then returns. The documented developer command is:

    ./gradlew totalcrossPreview --continuous

Gradle continuous build recompiles affected inputs and reruns the lightweight
notification task. Do not keep the preview worker inside the Gradle daemon.
Declare inputs and outputs precisely enough that ordinary unrelated changes do
not trigger reload.

In the Maven plugin, keep Maven lifecycle integration native. Add goals:

    totalcross:run
    totalcross:preview

The preview goal starts the external host, watches declared source and resource
roots with debounce, invokes the project wrapper or Maven executable with
`compile` when needed, and sends reload only after a successful build. Never
implement a Java compiler inside the plugin. Prevent recursive invocation with a
session environment marker.

Move Java version policy into shared tooling. Preserve the known distinction
between the build JVM, tooling JDK, and application classfile target. Preserve
legacy Retrolambda behavior for SDK versions that require it. Tests must cover
JDK 11/17 policy boundaries, explicit JDK path, modern SDK, legacy SDK, and
Retrolambda selection.

Create equivalent small Gradle and Maven sample projects. A shared assertion
reads their generated `ProjectModel` and compares semantic fields rather than
tool-specific paths.

## Surprises & Discoveries

- Observation: none recorded yet.
  Evidence: add only build-system behavior that changes the common model.

## Decision Log

- Decision: keep compile orchestration native to each build system.
  Rationale: Gradle continuous build and Maven lifecycle semantics differ, while
  the resulting session model can remain common.
  Date/Author: 2026-07-26 / OpenAI.

- Decision: keep the preview host outside build daemons.
  Rationale: closing or reloading preview must not destabilize Gradle or Maven.
  Date/Author: 2026-07-26 / OpenAI.

## Validation and Acceptance

Run shared model tests, focused plugin unit tests, then one Gradle and one Maven
functional preview fixture. Acceptance requires:

    ./gradlew totalcrossPreview --continuous

to compile, start or reuse the host, and notify after a source edit; and:

    ./mvnw totalcross:preview

to compile, start preview, rebuild after a source edit, and not recurse. Both
must produce equivalent session-model semantics and use shared JDK policy.

Do not run all plugin matrices until closing this plan. Run the staged size
checker and `git diff --check`.

## Risks and Open Questions

Gradle continuous tasks may run concurrently with host startup. Use a locked
session record and recheck host health. Maven watch loops must avoid reacting to
their own build outputs. Normalize and exclude output directories explicitly.

## Idempotence and Recovery

Starting preview twice reuses a compatible session or reports the conflicting
session and provides `previewStop`. Failed compilation leaves the current worker
running and reports that reload was skipped. A successful later build can retry
without deleting the session.

## Outcomes & Retrospective

Not started.

## Revision Note

2026-07-26: defined equivalent Gradle continuous and Maven watch workflows while
keeping compilation native to each tool.

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
