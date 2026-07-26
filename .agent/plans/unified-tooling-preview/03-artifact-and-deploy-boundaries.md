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

- [ ] Inventory package ownership and runtime dependencies.
- [ ] Add filtered artifact tasks without moving Java source.
- [ ] Add artifact-content contract tests.
- [ ] Create typed deploy request, result, and service interfaces.
- [ ] Implement the legacy `tc.Deploy` adapter through an isolated classpath.
- [ ] Migrate one focused tooling path to the typed service.
- [ ] Preserve the aggregate SDK artifact and compatibility entry points.
- [ ] Commit both repository checkpoints and update state to Plan 04.

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

- Observation: none recorded yet.
  Evidence: update only with findings that affect later migration.

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

Not started.

## Revision Note

2026-07-26: made artifact separation the mandatory pre-move checkpoint and
defined a typed deploy boundary with a legacy adapter.

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
