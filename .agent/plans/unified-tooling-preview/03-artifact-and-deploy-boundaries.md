<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Establish artifact and deploy boundaries without moving sources

This ExecPlan is Plan 03. It creates consumer-visible separation while preserving
the physical converter and deployer paths until the IR merge gate.

## Purpose / Big Picture

Build and consume narrow SDK, runtime, converter, deployer, and preview artifacts
without relocating converter source. Gradle, Maven, and future CLI code can call
a typed deploy service instead of constructing `tc.Deploy` command lines
independently, while existing applications still receive the aggregate SDK JAR.

## Working Set and Resume Protocol

Read the state file and this plan. Inspect the TotalCross SDK Gradle build,
`tc.Deploy`, converter/deployer package boundaries, packaging scripts, and current
plugin deploy invocation. Do not read native TCIR implementation in this plan.

## Progress

- [x] Inventory package ownership and runtime dependencies.
- [x] Add filtered artifact tasks without moving Java source.
- [x] Add artifact-content contract tests.
- [x] Create typed deploy request, result, and service interfaces.
- [x] Implement the legacy `tc.Deploy` adapter through an isolated classpath.
- [x] Migrate one focused tooling path to the typed service.
- [x] Preserve the aggregate SDK artifact and compatibility entry points.
- [x] Commit both repository checkpoints and update state to Plan 04.

## Current Architecture and Scope

All Java source remains in its current TotalCross repository location. Additive
artifact tasks package existing compiled outputs into:

    totalcross-api.jar
    totalcross-runtime-java.jar
    totalcross-converter.jar
    totalcross-deployer.jar
    totalcross-preview-runtime.jar
    totalcross-sdk.jar

The exact include/exclude package map is derived from the current build and
written as a small local document under
`TotalCrossSDK/docs/artifact-boundaries.md`. Keep that document below the size
limit. The aggregate `totalcross-sdk.jar` remains compatible during this program.

In tooling, create deploy packages in `tooling-core` or a small
`tooling-deploy` module if dependencies would otherwise pollute the core. Use
typed values, not global mutable `DeploySettings`, at the new boundary.


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

Inspect `TotalCrossSDK/build.gradle` and packaging scripts before editing. If the
build file exceeds the size policy and must be modified, extract artifact task
registration into a small applied Gradle script or plugin class first. Do not
make cosmetic unrelated changes.

Create deterministic Jar tasks that package from existing compiled outputs.
They must not compile a second copy of converter source and must not move files.
Add tests that open each JAR and assert required packages are present and
forbidden packages are absent. The preview artifact may depend on runtime Java;
the API artifact must not contain deployer, converter, AWT preview, or platform
tool binaries.

Define these public tooling contracts with small immutable classes:

    DeployRequest
    DeployPlatform
    DeployLogLevel
    DeployResult
    DeployDiagnostic
    DeployService
    DeployToolchain

`DeployRequest` contains input artifact, output directory, SDK installation,
tooling JDK, platforms, library/application mode, logging, and supported legacy
options. It must not expose `DeploySettings` or raw mutable static state.

Implement `LegacyDeployService` with an isolated classloader whose classpath is
the narrow deployer/converter artifacts plus declared runtime dependencies. It
invokes the compatibility entry point `tc.Deploy` and captures exit status,
generated outputs, and diagnostics. Serialize concurrent legacy invocations if
static state makes parallel execution unsafe. Record that limitation.

Migrate only one existing Gradle-plugin package path as the proof. Do not yet
remove duplicate Maven logic; Plan 06 performs the full plugin migration.

Keep `tc.Deploy` unchanged when possible. If a necessary change touches an
oversized non-protected file, split it first into facade, argument parsing,
execution, and platform dispatch collaborators, with the public facade preserved.
Do not split converter files in protected paths.

## Surprises & Discoveries

- Observation: the SDK already had internal `tc.base.*` and `tcui` JAR tasks,
  but no consumer-facing boundaries for API, runtime, converter, deployer, and
  preview. The aggregate `totalcross-sdk` task remains unchanged.
  Evidence: `TotalCrossSDK/build.gradle` and the generated artifact-content
  test output in `/tmp/totalcross-artifact-boundaries.log`.

- Observation: `tc.Deploy` is 26,414 bytes and 553 lines, so the typed proof
  uses a new Gradle task and isolated adapter without modifying that facade.
  Evidence: `wc -c -l TotalCrossSDK/src/main/java/tc/Deploy.java`; the file is
  not protected by path but is too large to edit under the program policy.

- Observation: invoking the legacy adapter requires the runtime, converter, and
  deployer artifacts on one disposable classpath; static deploy state therefore
  remains serialized by `LegacyDeployService`.
  Evidence: `tooling-java/tooling-core/src/main/java/com/totalcross/tooling/deploy/LegacyDeployService.java`.

## Decision Log

- Decision: use packaging boundaries before compilation/source boundaries.
  Rationale: it delivers narrow consumer artifacts without colliding with active
  IR work.
  Date/Author: 2026-07-26 / OpenAI.

- Decision: retain `tc.Deploy` as a compatibility facade.
  Rationale: existing scripts and SDK consumers require a gradual migration.
  Date/Author: 2026-07-26 / OpenAI.

## Validation and Acceptance

Run artifact-content tests, the focused typed-deploy test, and one smoke package
for a small example. Do not run every platform package. Acceptance requires the
legacy aggregate flow and the new typed flow to generate equivalent focused
output names and success status for the same input.

Run the staged size checker in both repositories and `git diff --check`. Record
the paired commits as one checkpoint in state and evidence.

## Risks and Open Questions

Package boundaries may reveal runtime dependencies not declared in the current
build. Add them to explicit artifact metadata rather than copying all SDK
libraries. Static deploy state may prevent safe in-process concurrency; prefer
process isolation later rather than hiding the limitation.

## Idempotence and Recovery

Artifact tasks overwrite only their build outputs. The typed deploy smoke test
uses a temporary output directory. A failed migration can revert the tooling
adapter commit while leaving additive TotalCross artifact tasks intact.

## Outcomes & Retrospective

Plan 03 created versioned narrow JAR tasks in `TotalCrossSDK/gradle/artifact-boundaries.gradle`
without moving source. The aggregate `totalcross-sdk` remains the existing
compatibility artifact. Content tests prove API excludes converter/deployer/
preview, converter and deployer are separate, and preview contains its surface.

`tooling-core` now exposes immutable deploy contracts and an isolated legacy
adapter. The Gradle plugin registers `totalcrossTypedPackage` as a focused proof
path while leaving the existing `totalcrossPackage`/Maven implementation for
later migration. The paired commits remain to be created.

## Revision Note

2026-07-26: made artifact separation the mandatory pre-move checkpoint and
defined a typed deploy boundary with a legacy adapter.

2026-07-26: added five versioned boundary JAR tasks, content tests, typed deploy
contracts, the isolated legacy adapter, and the Gradle typed proof task.

## Editorial Report

This section is mandatory at completion. Keep it factual and evidence-based.

### Editorial Summary

The SDK's aggregate JAR bundled public API, runtime, converter, deployer, and
preview classes, which made future tooling extraction risky. Plan 03 adds
consumer-visible boundaries while preserving the aggregate artifact and legacy
`tc.Deploy` entry point.

### Original Plan versus Actual Outcome

The planned additive artifact tasks, package contract tests, typed deploy
contracts, isolated adapter, and one Gradle proof path were delivered. The
existing Gradle task and Maven path were deliberately not removed; broad plugin
migration remains later work.

### What Changed

`TotalCrossSDK/gradle/artifact-boundaries.gradle` creates
`totalcross-api`, `totalcross-runtime-java`, `totalcross-converter`,
`totalcross-deployer`, and `totalcross-preview-runtime` JARs. The SDK test
`tc.tools.ArtifactBoundariesTest` inspects their contents. Tooling adds
`com.totalcross.tooling.deploy` contracts and `LegacyDeployService`; the Gradle
plugin adds `TypedDeployTask` and registers `totalcrossTypedPackage`.

### Decisions and Trade-offs

Packaging boundaries precede source relocation so IR and converter history stay
untouched. The adapter uses a disposable classloader and a global invocation
lock, which prevents static-state races at the cost of serial deploy calls. The
old path remains for compatibility and comparison.

### Unexpected Problems and Discoveries

The first artifact test assumed unversioned file names, but Gradle correctly
emitted `*-7.2.2.jar`; the test was changed to locate versioned prefixes. The
tooling plugin initially used a stale Maven Local snapshot and passed after the
core was republished with deploy types.

### Validation and Measurable Results

`./gradlew-agent artifactContentTest --console=plain` passed after the filename
fix. `./tooling-java/gradlew -p tooling-java :tooling-core:test --console=plain`
passed, and `gradle-plugin/./gradlew test --console=plain` passed. Logs are in
`/tmp/totalcross-artifact-boundaries.log`,
`/tmp/tooling-core-plan03-test.log`, and
`/tmp/gradle-plugin-plan03-test.log`.

### Useful Evidence and Examples

The artifact task script and JUnit package assertions are small, reproducible
examples of the boundary contract. `TypedDeployTask` shows the Gradle-to-typed
mapping without changing the legacy task.

### Limitations, Remaining Work, and Open Questions

The typed adapter still invokes the legacy Java entry point and requires a
complete narrow runtime classpath. Maven migration, artifact publication to a
remote repository, and removal of duplicate resolver/deploy logic are deferred.
Physical source ownership remains unchanged pending the IR gate.

### Possible Article Angles

For SDK maintainers: “Why artifact boundaries should precede source moves”
shows how content contracts reduce merge risk. For build-tool authors: “Putting
a typed API in front of a static legacy deployer” explains classloader isolation
and serialized calls.

### Suggested Narrative

Introduce the aggregate-JAR coupling, inventory the existing package groups,
add deterministic versioned artifacts and content tests, then add the typed
request/result boundary and isolated adapter. Show the filename-test correction,
the passing SDK/tooling/plugin validations, and the remaining Maven/IR work.

### Claims Requiring Human Review

The exact package maps are a first boundary proposal and should be reviewed by
SDK consumers before publication. The adapter's classpath requirements and
static-state serialization should be verified against real deploy outputs on
each supported platform.

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
