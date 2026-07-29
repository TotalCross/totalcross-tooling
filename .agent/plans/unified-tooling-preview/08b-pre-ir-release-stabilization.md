<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Stabilize the pre-IR release

This ExecPlan is Plan 08B. It converts existing modules and plugin checkpoints
into one production end-to-end flow. It must complete before release branches
are created.

## Purpose / Big Picture

Provide one preview lifecycle used by CLI, Gradle, Maven, and VS Code; remove
public placeholders and duplicated resolvers; preserve existing SDK usage; and
prove the complete release candidate behavior before any IR merge.

## Working Set and Resume Protocol

Read state and this plan. Inspect only:

    tooling-java CLI, protocol, host, worker, and reload coordinator
    live-preview-server HTTP/Webview adapter
    Gradle run, preview, stop, project model, package, and resolvers
    Maven run, preview, stop, package, JDK managers, and POM
    VS Code preview commands, process clients, wizard, and conversion workflow
    SDK aggregate and narrow artifact publication configuration
    compatibility tests

Do not merge IR, move converter source, tag, or publish publicly.

## Progress

- [ ] Declare `tooling-java` host/worker as the production preview lifecycle.
- [ ] Convert the HTTP server into an authenticated presentation adapter or mark it legacy.
- [ ] Complete CLI project discovery, worker launch, reload, stop, and JSON events.
- [ ] Implement worker resize, pointer, key, and reload commands.
- [ ] Make Gradle preview, run, stop, and package use shared tooling.
- [ ] Make Maven preview, run, stop, and package use shared tooling.
- [ ] Make shared SDK/JDK/Java/Retrolambda policy authoritative.
- [ ] Resolve Maven JVM versus Java-17 tooling compatibility.
- [ ] Consolidate VS Code preview commands and companion installation.
- [ ] Add multi-root selection, input forwarding, and build-before-reload.
- [ ] Preserve wizard and Maven-to-Gradle migration behavior and rollback.
- [ ] Publish aggregate and narrow artifacts to a local staging repository.
- [ ] Prove aggregate-SDK compatibility and preview version gating.
- [ ] Pass the pre-IR end-to-end matrix.
- [ ] Commit and update state to Plan 08R.

## Current Architecture and Scope

The production lifecycle is:

    build tool or VS Code
        -> shared coordinator
        -> persistent host
        -> authenticated disposable worker
        -> SDK preview runtime adapter

There must not be a second implementation that owns application classloaders,
reload state, or worker lifecycle. The HTTP/Webview component may translate
frames and input, but it delegates session ownership to the shared coordinator.

The aggregate `totalcross-sdk.jar` remains supported. Narrow artifacts are
additive. Preview is enabled only for SDK versions that contain the required
preview contract; older versions receive a clear compatibility diagnostic.


## Cross-plan safety and size policy

Run one plan at a time. Preserve unrelated work. Never use `git reset --hard`,
`git clean -fd`, force-push, history rewriting, tag deletion, or repository
archival unless the user explicitly requests that exact operation.

Every created or modified text file must remain at or below 20 KiB and at or
below approximately 600 lines. Run the staged size-policy checker before every
commit. If an existing non-protected file exceeds either limit, split it by
responsibility before the functional change. Do not split a protected IR-related
file merely to satisfy this rule.

Protected paths:

    TotalCrossSDK/src/main/java/tc/tools/converter/**
    TotalCrossSDK/src/test/java/tc/tools/converter/**
    TotalCrossVM/src/tcvm/ir/**
    TotalCrossVM/src/tcvm/jit/**
    TotalCrossVM/src/tcvm/aot/**
    TotalCrossVM/src/tests/ir/**
    docs/architecture/bytecode/**

Generated files, third-party code, caches, and build output must not be committed.

Use token-efficient execution. Read the state file first, then the active plan.
Inspect only named paths. Store verbose output in `/tmp` or build artifacts and
record only concise results, commit IDs, and log paths. Do not repeatedly print
large plans, logs, generated files, or full repository diffs.

## Plan of Work

### Canonical preview

Connect `PreviewReloadCoordinator` to real host and worker processes. Complete
worker calls for resize, pointer, key, reload preparation, and replacement.
Candidate promotion still requires authentication, ready, and first valid frame.

Complete the CLI so `preview --project` and `run --project` read a versioned
project/session model, select the tooling JDK, launch the host and worker, stream
structured JSON diagnostics, accept reload/stop, and return meaningful exit
codes. Remove fixture-only behavior from public commands.

Change the HTTP server to connect to the shared session. Add session
authentication. Remove its independent classloader reload path or label that path
internal and unsupported for the release.

### Gradle

Make these tasks functional:

    totalcrossProjectModel
    totalcrossPreview
    totalcrossPreviewStop
    totalcrossRun
    totalcrossPackage

`totalcrossPreview` compiles first, starts or reuses the external coordinator,
and notifies only after a successful build. `--continuous` must not retain the
host inside the Gradle daemon. `totalcrossPreviewStop` stops the owned session,
not only a descriptor file. `totalcrossPackage` uses `DeployService`; remove the
public proof task or keep it internal to tests.

Replace hardcoded model values with actual source roots, class outputs,
dependencies, main class, Java target, SDK version, and tooling JDK.

### Maven

Provide equivalent goals and project-model semantics. Maven may implement a
bounded watch loop that invokes its normal compile lifecycle; it must not compile
Java itself or recurse into its preview goal.

Resolve Java compatibility explicitly. Preferred solution: keep Maven plugin
loading compatible with its supported Maven JVM and launch Java-17 tooling in a
subprocess. If direct loading is retained, raise the documented minimum JVM and
choose a semantically appropriate major plugin version.

Remove old SDK/JDK download paths after shared-core usage is proven. Remove
unused AWS/appdirs/zip dependencies.

### VS Code

Expose one public command set:

    TotalCross: Preview
    TotalCross: Run
    TotalCross: Stop Preview
    TotalCross: Preview Diagnostics

Keep the Gradle project wizard and Maven-to-Gradle conversion, including atomic
writes, backup, validation, rollback, and workspace opening.

Do not require users to configure `extraClasspath`. Bundle the companion or
install a checksummed distribution through the shared store. Select a project
from the active editor or prompt in multi-root workspaces. Delegate compilation
to Gradle or Maven before reload. Forward mouse, keyboard, resize, and supported
touch events.

### Artifact compatibility

Create real Maven publications for narrow artifacts with dependency metadata.
Do not treat the current limited API JAR as a replacement for the aggregate SDK.
Add `japicmp` or Revapi comparison between the prior aggregate SDK and the new
aggregate SDK. Test ordinary source/binary usage and focused reflection-sensitive
Launcher behavior.

Use a local staging repository, not `mavenLocal`, for final tests. All components
must resolve through repository declarations identical to the planned public
layout.

## Decision Log

- Decision: one production lifecycle; HTTP is an adapter.
  Rationale: two reload owners create incompatible behavior and distribution.
  Date/Author: 2026-07-28 / OpenAI.

- Decision: shared tooling policy is authoritative.
  Rationale: Gradle and Maven must not choose different SDKs, JDKs, targets, or
  Retrolambda behavior.
  Date/Author: 2026-07-28 / OpenAI.

- Decision: preserve aggregate SDK compatibility and version-gate preview.
  Rationale: packaging compatibility and new preview capability are separate.
  Date/Author: 2026-07-28 / OpenAI.

## Validation and Acceptance

Run equivalent sample projects for Gradle and Maven. For each:

1. start preview;
2. receive first frame;
3. change Java source;
4. compile and promote a new worker;
5. introduce a compile error and preserve the old worker;
6. repair the source and reload;
7. change a resource and reload;
8. forward pointer, key, and resize;
9. stop and confirm no worker remains;
10. package through typed deploy.

Install the VSIX and repeat create-project, preview, edit/reload, stop, Maven
conversion, successful validation, forced validation failure, and rollback.

Test from empty temporary Gradle/Maven caches and a fresh shared store. No public
component may depend on a SNAPSHOT or `mavenLocal`. Run aggregate-SDK compatibility
tests and document preview's minimum SDK.

Run size checks, `git diff --check`, focused tests, then the justified full matrix
on available macOS ARM64, Windows, and Linux hosts. Record unavailable hosts.

## Risks and Open Questions

The extension may need a bundled Java companion versus first-use installation.
Choose one release path and test the installed VSIX, not only Extension Host.

Maven compatibility may require subprocess isolation earlier than planned. Do not
hide a Java-17 classloading failure behind documentation.

## Idempotence and Recovery

Sessions have stable ownership metadata and stop commands. Failed builds do not
replace active workers. Local staging uses unique version directories. Each
integration slice is separately revertible.

## Outcomes & Retrospective

Not started.

## Revision Note

2026-07-28: added release stabilization for the real CLI/plugin/editor flow,
canonical preview architecture, compatibility, and clean staging consumption.

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
