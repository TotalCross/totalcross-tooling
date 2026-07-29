<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Correct the pre-IR preview and plugin architecture

This ExecPlan is Plan 08C. It is a release-blocking correction after the Plan
08B audit. Start from the accepted feature commits; do not create release
branches until this plan passes.

## Purpose / Big Picture

Make implemented behavior match the declared architecture: a persistent
coordinator promotes disposable worker processes only after a valid first frame,
and failed builds or candidates preserve the previous application.

Make Gradle, Maven, CLI, and VS Code consume the same project model, store, JDK
selection, compatibility policy, and public tasks. Correct extension reload,
input, resize, wrapper, and packaging behavior before adding new migration UX.

## Working Set and Resume Protocol

Read state and this plan. Inspect only:

    tooling-java host, worker, coordinator, CLI, codecs, and resolvers
    Gradle preview, run, stop, model, package, extension, and build metadata
    Maven preview, run, stop, package, JDK/SDK managers, POM, and tests
    VS Code preview client, Webview, project layout, settings, and packaging
    TotalCross preview contract documentation and focused tests
    current state and evidence

Do not implement the new project converter in this plan. Do not merge IR, move
converter source, create tags, or publish publicly.

## Progress

### Reconciliation

- [ ] Record current remote heads and remove stale local-only SHAs from state.
- [ ] Replace absolute machine paths with repository-relative resume commands.
- [ ] Record the post-08B audit findings in evidence.

### Process-isolated preview

- [ ] Add a process-backed candidate implementation.
- [ ] Connect `PreviewReloadCoordinator` to production CLI execution.
- [ ] Keep one coordinator alive while worker candidates are replaced.
- [ ] Require authentication, ready, and first frame before promotion.
- [ ] Preserve the active worker after timeout, error, or invalid frame.
- [ ] Route input and stop only to the promoted worker.
- [ ] Close the previous worker and descendants only after promotion.
- [ ] Prove repeated reload leaves no old worker processes.
- [ ] Define distinct preview and run presentation modes.

### Shared resolution and model

- [ ] Make shared SDK/JDK/store services authoritative in Gradle, Maven, and CLI.
- [ ] Remove the Maven Zulu-only, latest, x86 JDK download path.
- [ ] Run capability probes before using a tooling JDK.
- [ ] Enforce and document Java 17 for loading both pre-IR plugins.
- [ ] Serialize and parse the complete versioned ProjectModel.
- [ ] Include real roots, outputs, dependencies, SDK, targets, and arguments.
- [ ] Remove or internalize `totalcrossTypedPackage`.
- [ ] Remove obsolete resolver/download dependencies after equivalence tests.
- [ ] Clarify internal versus external preview contract documentation.

### VS Code corrections

- [ ] Build successfully before requesting a candidate reload.
- [ ] Keep the displayed frame and session after a failed build.
- [ ] Prefer Maven Wrapper when present.
- [ ] Scale pointer coordinates to intrinsic frame pixels.
- [ ] Send resize and apply device size, density, and orientation.
- [ ] Bundle production dependencies deterministically into the VSIX.
- [ ] Remove SNAPSHOT and `publishToMavenLocal` from release defaults.
- [ ] Pass focused and installed-VSIX E2E tests.
- [ ] Commit focused slices and update state to Plan 08D.


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

## Plan of Work

Execute one milestone at a time. Commit each accepted milestone before reading
implementation paths for the next.

### Milestone 1: reconcile state and evidence

Update state with current refs, active blockers, and a relative next command.
Preserve Plan 08B evidence. Record:

    production reload reuses one worker
    VS Code stops before build
    Gradle and Maven still own duplicate resolver paths
    Maven JDK manager is Zulu-only and x86-biased
    project model writes only the preview descriptor
    duplicate typed package task remains public
    Webview input scaling and resize are incomplete
    release files still use SNAPSHOT or mavenLocal

### Milestone 2: process-backed candidate promotion

Introduce focused classes, for example:

    PreviewCoordinator
    ProcessWorkerCandidate
    ActivePreviewSession
    PresentationMode
    WorkerProcessFactory

Do not grow `ToolingCli` into the coordinator implementation.

A process candidate owns one authenticated worker and transport. It starts,
awaits authentication, awaits ready, validates the first frame, exposes
diagnostics, and closes its complete process tree.

The persistent coordinator keeps the active candidate visible and controllable
while a new candidate starts with the new compiled classpath. It promotes only
after a valid frame, then closes the previous process. Failed candidates emit a
structured diagnostic and never replace the active process.

Use two presentation modes:

    preview:
      frame stream, frame file, or IDE adapter

    run:
      native AWT window owned by the coordinator

Both modes use the same worker lifecycle.

Replace the append-only comma control format with a versioned escaped message
format or authenticated local socket. Keep a compatibility reader only when
needed for current tests.

### Milestone 3: authoritative environment resolution

Create one `tooling-core` facade, for example
`ToolingEnvironmentResolver`, returning:

    SDK installation and version
    tooling JDK installation
    application Java target
    Retrolambda decision
    external tools
    diagnostic provenance

Use the immutable store, JDK providers, checksum metadata, and capability probes.
An explicit local path is the first candidate but must pass the same probes.

Gradle, Maven, and CLI call this facade. Remove the Maven `JavaJDKManager`
download path and Gradle-local JDK downloader only after focused equivalence
tests. Remove unused AWS, appdirs, zip, and download dependencies.

Both plugins load on Java 17 for this release. Add startup diagnostics, README
matrices, Gradle tests, Maven prerequisite metadata where supported, and failure
tests for older JVMs. Application bytecode target remains independent.

### Milestone 4: complete project model and tasks

Add a versioned `ProjectModelCodec`. Serialize the entire model:

    schema version
    build tool
    project root
    main and test source roots
    resource roots
    class and resource outputs
    dependency classpath
    MainWindow class
    SDK version and coordinate
    tooling JDK identity
    application Java target
    Retrolambda plan
    Launcher arguments
    Deploy arguments and platforms
    preview descriptor

Do not store credentials or unnecessary absolute cache paths.

Gradle and Maven must produce semantically equivalent models. CLI and VS Code
consume the model rather than rediscovering values differently.

Keep only `totalcrossPackage` and `totalcross:package` as public package entry
points. Remove or internalize `totalcrossTypedPackage`. Document `preview`,
`run`, and `stop` semantics consistently.

Document SDK in-process preview types as internal compatibility surfaces. The
cross-process frame and command protocol is the stable external boundary.

### Milestone 5: correct VS Code preview

Reload order:

    build
      failure -> report error and retain current session and frame
      success -> request a candidate reload
    await promoted event
    update diagnostics and frame

Prefer wrappers:

    Gradle: gradlew or gradlew.bat
    Maven: mvnw or mvnw.cmd
    system executable only when no wrapper exists

Track intrinsic frame dimensions and displayed dimensions. Scale pointer
coordinates before sending. Use `ResizeObserver` and device-profile settings to
send width, height, density, and orientation. Remove or deprecate unused settings.

Bundle the extension with a supported bundler or a deterministic dependency
package. Inspect and install the VSIX without source checkout or development
`node_modules`.

### Milestone 6: validation

Acceptance requires:

1. twenty process-replacement reloads leave only the active worker;
2. candidate failure preserves prior frame and process;
3. VS Code build failure preserves active preview;
4. pointer scaling and resize reach the worker correctly;
5. Gradle, Maven, CLI, and VS Code consume equivalent project models;
6. no public plugin path uses legacy JDK/SDK downloaders;
7. only one public package task exists per build tool;
8. installed bundled VSIX passes preview, reload, failed build, recovery, stop;
9. no SNAPSHOT or `mavenLocal` remains in release-facing defaults.

Run focused tests per milestone, then the justified full matrix. Run
`git diff --check`, license checks, dependency analysis, and staged size checks.

## Decision Log

- Decision: worker replacement is a release gate.
  Rationale: MainWindow replacement does not guarantee application state
  isolation.
  Date/Author: 2026-07-29 / OpenAI.

- Decision: plugin loading requires Java 17 for the pre-IR release.
  Rationale: current plugin and shared tooling bytecode already require it.
  Date/Author: 2026-07-29 / OpenAI.

- Decision: project conversion is deferred to Plan 08D.
  Rationale: architecture correction and migration analysis are separate bounded
  contexts.
  Date/Author: 2026-07-29 / OpenAI.

## Validation and Acceptance

Plan 08C completes only when every progress item and Milestone-6 acceptance item
passes. Coordinator unit tests alone are insufficient; the real CLI, plugins,
and installed extension must use process-backed promotion.

Record unavailable platform tests honestly.

## Risks and Open Questions

Changing process ownership may expose existing assumptions in detached Maven or
Gradle sessions. Prefer one shared session record and authenticated control
channel over PID parsing.

## Idempotence and Recovery

Each milestone is separately commit-ready and revertible. Candidate failure does
not change active preview ownership. Plan 08R remains blocked.

## Outcomes & Retrospective

Not started.

## Revision Note

2026-07-29: created from the post-Plan-08B audit.


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
