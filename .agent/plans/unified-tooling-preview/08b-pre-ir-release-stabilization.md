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

- [x] Declare `tooling-java` host/worker as the production preview lifecycle.
- [x] Convert the HTTP server into an authenticated presentation adapter or mark it legacy.
- [x] Complete CLI project discovery, worker launch, reload, stop, and JSON events.
- [x] Implement worker resize, pointer, key, and reload commands.
- [x] Make Gradle preview, run, stop, and package use shared tooling.
- [x] Make Maven preview, run, stop, and package use shared tooling.
- [x] Make shared SDK/JDK/Java/Retrolambda policy authoritative.
- [x] Resolve Maven JVM versus Java-17 tooling compatibility.
- [x] Consolidate VS Code preview commands and companion installation.
- [x] Add multi-root selection, input forwarding, and build-before-reload.
- [x] Preserve wizard and Maven-to-Gradle migration behavior and rollback.
- [x] Publish aggregate and narrow artifacts to a local staging repository.
- [x] Prove preview version gating and its clear older-SDK diagnostic.
- [ ] Prove aggregate-SDK compatibility against the public baseline.
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

The canonical host/worker path, build-tool adapters, VS Code command surface,
shared deploy service, narrow Maven publications, and a local staging repository
are implemented. The CLI now produces a real fixture PNG and accepts control-file
resize, pointer, key, and stop commands; VS Code polls that frame and forwards
the same events. Isolated Gradle and Maven projects now pass first frame, source
reload after a failed compile, resource processing, input controls, and stop
through their native goals. Clean-cache SDK/tooling resolution and a
checkpoint-based aggregate compatibility gate now pass. Release acceptance
remains open for typed deploy packaging, the public-baseline compatibility
decision, the complete VS Code matrix, and installed-VSIX acceptance.

## Revision Note

2026-07-28: added release stabilization for the real CLI/plugin/editor flow,
canonical preview architecture, compatibility, and clean staging consumption.

## Editorial Report

Complete this section only from executed evidence.

### Editorial Summary

The slice exceeded the original structural checkpoint by making Gradle and Maven
fork the shared CLI and by moving both package goals behind `DeployService`.
Artifact staging, the canonical CLI first-frame/control path, and native Gradle
and Maven reload/control/stop flows are proven locally. Release-level
compatibility, typed deploy packaging, and complete VS Code clean-environment
acceptance remain pending.

### Original Plan versus Actual Outcome

Implemented canonical worker launch/handshake, CLI stop, reflective reload and
input hooks, Gradle/Maven preview-stop, shared package execution, shared
SDK/JDK/Java/Retrolambda compatibility policy, VS Code
multi-root selection/build-before-reload scaffolding, legacy HTTP labeling, and
aggregate plus narrow SDK publication metadata.

### What Changed

The Maven plugin remains Java 17 at load time and uses a subprocess for the
tooling CLI. Existing SDK/JDK download managers remain only for compatibility
resolution; the actual deploy invocation is isolated through the shared core.

### Decisions and Trade-offs

The first CLI probe exposed missing SDK runtime dependencies and a symlinked
temporary-directory edge case; the worker diagnostics and frame writer now
report both cases clearly. The SDK also emits an initial frame for headless
consumers, independent of application-specific repaint calls.
The full Maven suite was not run because legacy cache tests concurrently started
large network downloads; focused Maven tests and packaging passed.

### Unexpected Problems and Discoveries

The Gradle daemon does not expose the plugin dependency classpath as a usable
`java.class.path`; the fork now collects code-source locations for CLI, host,
worker, and protocol. Legacy deploy fixtures also use a static `main` rather
than the production constructor, so the adapter accepts both entry shapes.

### Validation and Measurable Results

Passed: tooling-java full tests (14 tests), Gradle plugin full tests (21 tests),
focused Maven tests and package, live-preview-server tests with SDK 7.2.2 (3
tests), VS Code integration suite (30 tests), SDK `compileJava`, license checks,
`git diff --check`, SDK aggregate/narrow publication to `TotalCrossSDK/build/repo`,
and tooling-java publication to `/tmp/totalcross-plan08b-staging`. The direct
CLI fixture produced `/tmp/totalcross-cli-frame.png` as a 320x568 PNG; a second
control-file run accepted resize, pointer, key, and stop commands.
Temporary Gradle and Maven projects compiled the same fixture, produced a
non-empty 320x568 preview frame, and stopped through `totalcrossPreview` /
`totalcross:preview`; the Maven frame was
`/tmp/totalcross-maven-e2e.i20RTr/target/totalcross/preview-frame.png`.
The isolated Gradle and Maven matrix also preserved the active coordinator after
a failed compile, reloaded repaired classes, processed a changed resource,
accepted resize/pointer/key commands, and stopped without a remaining process.
The shared compatibility policy tests passed in tooling-core; Gradle plugin
tests, Maven compilation/package, targeted Retrolambda coverage, and JDK 17
selection coverage also passed. A local VSIX was packaged and installed in the
real VS Code, and the existing integration suite passed 30 tests; the suite
still loads the development extension path, so installed-VSIX activation remains
open.
The clean Maven repository `/tmp/totalcross-clean-m2.Symu0K` resolved SDK,
preview-runtime, and tooling CLI artifacts from staged repositories without
`mavenLocal`; the required annotations artifact was added to staging. japicmp
passed against a clean aggregate JAR built from checkpoint `a020512e4`.

### Useful Evidence and Examples

The staging repository contains POM/module metadata for tooling-core,
tooling-protocol, preview-host, preview-worker, and tooling-cli; the SDK staging
repository contains `totalcross-sdk` plus `totalcross-api`,
`totalcross-runtime-java`, `totalcross-converter`, `totalcross-deployer`, and
`totalcross-preview-runtime`.

### Limitations, Remaining Work, and Open Questions

The clean Gradle/Maven/VS Code matrix and installed-VSIX acceptance remain open.
The published Gradle plugin also resolved from isolated staging and reached
`totalcrossPackage`, but its real deploy smoke used a synthetic SDK home and was
stopped because that home lacked the complete distribution required by
`tc.Deploy`; the typed contract is covered by the functional suite, while a
release-grade real packaging run still requires a complete SDK home.
With the official 7.2.2 SDK archive, the adapter now isolates legacy non-daemon
threads and honors the requested SDK home (`e8c79a4`). Deterministic core,
Gradle, and Maven validations pass, but the real packaging smoke still ends in
an external `Connection reset`, so the typed-deploy gate remains open.
Preview version gating is proven against cached SDK 7.2.0: the CLI emits a
structured compatibility error and exits 1 when the required runtime contract
is absent. The separate aggregate binary compatibility decision remains open.
The aggregate check against 7.2.0 reports concrete public API removals and
classfile changes, so the current preview-capable floor proven locally is SDK
7.2.2 and public publication requires an explicit versioning/compatibility
decision.
The older cached 7.2.0 comparison still reports historical Launcher/deployer
incompatibilities and requires a release-owner compatibility decision; the
preceding checkpoint comparison is green. No IR merge, tag, push, or public
publication was performed.

### Possible Article Angles

The release should tell the story of one authenticated lifecycle crossing build
tools and editors, with legacy HTTP retained only as a compatibility adapter.

### Suggested Narrative

Start with the host handshake, follow the disposable worker through a verified
frame, then show Gradle/Maven/VS Code delegating to the same coordinator.

### Claims Requiring Human Review

Claims about installed-VSIX delivery, SDK version gating, and aggregate binary
compatibility require the remaining end-to-end evidence; the CLI first-frame and
control path are locally verified.
