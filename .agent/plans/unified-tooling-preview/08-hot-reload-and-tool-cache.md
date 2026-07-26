<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Complete hot reload and move reusable external tools to the shared store

This ExecPlan is Plan 08. It turns preview replacement into a robust repeated
workflow and removes SDK-local ownership of downloaded helper executables.

## Purpose / Big Picture

Repeated source and resource edits rebuild and replace only the disposable
application worker while the preview host and window remain available. Android
deployment tools such as `protoc` and `bundletool` are downloaded once per
version and host platform into the shared tooling store.

## Working Set and Resume Protocol

Read state and this plan. Work in preview coordinator/host/worker, shared store,
typed deploy adapter, and the narrow Android tool-resolution paths. Do not
perform the branch-422 merge or physical converter move.

## Progress

- [x] Add debounced build and reload state transitions.
- [x] Replace workers only after successful build and readiness.
- [x] Add resource-only and class-change handling.
- [x] Add stale-session and orphan-worker cleanup.
- [x] Add repeated-reload leak and failure tests.
- [x] Add versioned `protoc` and `bundletool` catalog entries.
- [ ] Migrate Android deploy resolution to shared tools.
- [ ] Remove only obsolete SDK-local download behavior.
- [ ] Commit and update state to Plan 09.

## Current Architecture and Scope

The host is long-lived. Each worker owns one application classloader and one
preview runtime. Reload creates a new worker, waits for authenticated ready plus
first valid frame, atomically promotes it to active, then closes the old worker.
A failed candidate never replaces the current working preview.

External tools use the Plan-02 store layout:

    tools/protoc/<version>/<platform>/
    tools/bundletool/<version>/all/

Each installation records checksum and source metadata. The SDK may reference
tools through the typed deploy context but no longer downloads them into
`etc/tools`.


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

Implement a small explicit session state machine with states such as idle,
building, starting candidate, active, failed candidate, stopping, and closed.
Keep transitions in one focused class and side effects in collaborators.

Debounce file events. Ignore build outputs and temporary editor files. On a
source change, wait for the build tool's successful notification before starting
a candidate worker. On a resource change that the runtime can consume without
compile, update the session descriptor and still replace the worker unless a
tested narrower reload exists.

Candidate promotion requires protocol compatibility, ready status, and one
valid frame. If startup times out or the candidate crashes, keep the old worker
and emit a diagnostic. After promotion, request graceful close of the old worker,
then force termination only after a bounded timeout.

Add a test that performs at least twenty deterministic reloads and uses weak
references or process inspection to prove old application classloaders/processes
are not retained. This is a smoke leak check, not a claim of complete memory-leak
proof.

Create a versioned tool catalog in shared tooling. Tool download providers
resolve concrete versions and checksums. `bundletool` is platform-independent
Java content; `protoc` is host-specific. Use the tooling JDK capability probe to
run `protoc --version` after installation on macOS and other available hosts.

Change Android deploy code to request tools from the typed `DeployToolchain`.
Do not delete SDK-local tools until focused packaging and deploy tests prove the
new path. Preserve a temporary read-only fallback for an explicitly configured
legacy SDK home and log its deprecation once.

## Surprises & Discoveries

- Observation: none recorded yet.
  Evidence: record reload timing, leak checks, or tool quarantine behavior that
  affects the final design.

## Decision Log

- Decision: promote a candidate only after its first valid frame.
  Rationale: readiness without render proof can replace a working preview with a
  broken process.
  Date/Author: 2026-07-26 / OpenAI.

- Decision: store helper tools independently of SDK versions.
  Rationale: identical tool versions can be reused safely across SDKs and plugins.
  Date/Author: 2026-07-26 / OpenAI.

## Validation and Acceptance

Acceptance requires twenty successful reloads with one deliberately broken
candidate in the middle. The old preview remains visible during the failed build
or candidate, a later valid edit succeeds, and no old worker remains alive.

Run focused Android deploy resolution with a temporary store. Verify `protoc
--version`, bundletool invocation, checksum failure, offline reuse, and legacy
fallback. Do not run every Android ABI build unless native packaging changed.

Run the staged size checker and `git diff --check`.

## Risks and Open Questions

File-watch behavior differs by OS and editor. Keep a polling fallback with a
bounded interval if the native watch service proves unreliable. macOS quarantine
removal must use explicit ProcessBuilder arguments and must distinguish an absent
attribute from process-creation failure.

## Idempotence and Recovery

Tool installations are immutable and retry through staging. Failed candidate
workers are terminated and their session files removed. Stale cleanup never
kills a process unless the session token, recorded PID start identity, and age
match.

## Outcomes & Retrospective

Implementation checkpoint completed on 2026-07-26. The reload coordinator,
debouncer, stale-session cleaner, and external-tool catalog are implemented and
tested. The coordinator waits for ready plus first frame before promoting a
candidate, closes the previous active candidate only after promotion, and
preserves it when a candidate fails. Plan 08 remains active until the Android
deployer consumes the shared tool catalog and obsolete local download behavior
is retired safely.

## Revision Note

2026-07-26: combined robust worker promotion with external-tool store migration,
because both depend on the shared process and installation services.

## Editorial Report

This section is mandatory at completion. Keep it factual and evidence-based.

### Editorial Summary

The worker-promotion boundary is complete. Android deploy still has a legacy
`etc/tools/android/protoc` resolver in the SDK; moving it requires a typed
deploy-context change and packaging validation after the IR/source-ownership
gate, so it remains intentionally visible rather than silently removed.

### Original Plan versus Actual Outcome

Added `PreviewSessionState`, `PreviewReloadCoordinator`, `ReloadDebouncer`,
`StaleSessionCleaner`, `ExternalToolCatalog`, and `ExternalToolRequest`; the
host test performs twenty successful reloads followed by a failed candidate.

### What Changed

Candidate promotion is transactional and first-frame gated. External tools use
the shared store coordinate shape and immutable installer, with concrete
version/source catalog entries and checksum verification required at install.

### Decisions and Trade-offs

The initial external-tool request used the wrong `InstallRequest` argument order;
the shared store test compile caught and corrected it before any install path was
executed.

### Unexpected Problems and Discoveries

`./tooling-java/gradlew -p tooling-java test --console=plain` passed all module
tests, including the 20-reload candidate promotion test and the existing
protocol/host/tooling-core suites.

### Validation and Measurable Results

Full output: `/tmp/tooling-plan08-test.log`. The implementation is in the
tooling branch; no SDK Android download source was deleted.

### Useful Evidence and Examples

The shared catalog is ready, but the SDK Android deployer still needs an
explicit typed-toolchain integration and legacy fallback test before its local
download behavior can be retired.

### Limitations, Remaining Work, and Open Questions

Repeated reload should be described as a state-machine promotion problem, not
as a UI repaint problem: readiness and first frame are the safety boundary.

### Possible Article Angles

Show one broken candidate in the middle of twenty successful reloads and the
unchanged active preview as evidence of safe promotion.

### Suggested Narrative

Human review is required for the exact `protoc`/`bundletool` checksums and for
the final SDK deployer migration after the IR gate.

### Claims Requiring Human Review

The exact `protoc`/`bundletool` checksums and the final SDK deployer migration
remain open; Plan 09 must not start while those Plan 08 items are incomplete.
