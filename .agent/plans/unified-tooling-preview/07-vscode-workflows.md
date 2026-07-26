<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Preserve and extend the VS Code workflows

This ExecPlan is Plan 07. It integrates the shared tooling without removing the
extension's existing project creation and migration responsibilities.

## Purpose / Big Picture

VS Code users keep the Gradle project wizard and automatic Maven-to-Gradle
conversion, including validation, backup, and rollback. They also gain preview,
run, stop, device-profile, and diagnostic commands backed by the same tooling
used from the command line and build plugins.

## Working Set and Resume Protocol

Read state and this plan. Inspect only the extension command registration,
project generator, Maven reader/converter, migration rollback, settings, tests,
and preview UI paths. Do not move user interaction into Java tooling.

## Progress

- [ ] Characterize current wizard and conversion behavior.
- [ ] Keep generation and migration tests passing before integration.
- [ ] Add a small CLI/session client layer.
- [ ] Add preview, run, stop, and reload commands.
- [ ] Preserve device, SDK version, and platform selection.
- [ ] Surface structured diagnostics and session status.
- [ ] Add extension integration tests.
- [ ] Commit and update state to Plan 08.

## Current Architecture and Scope

The extension remains responsible for:

    Gradle project creation wizard
    Maven-to-Gradle conversion command
    confirmation and progress UI
    backup and rollback presentation
    workspace selection and opening
    SDK version and platform selection
    preview panel or window coordination
    device profile and input controls
    diagnostics and secure credential interaction

Shared tooling may provide a project parser, rendered build model, version
catalog, migration plan, validator, preview session protocol, and CLI. It must
not silently replace the extension's interaction flow.


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

Add characterization tests around the current create-project command and
Maven-to-Gradle conversion. Verify generated Gradle files, POM backup naming,
atomic writes, rollback after validation failure, and the special diagnostic
when the Gradle plugin is unavailable.

Create a small TypeScript client with no UI code. It locates the project wrapper
or shared CLI, starts commands with `shell: false`, reads versioned JSON events,
and maps them to extension diagnostics. Keep process invocation, session state,
and UI commands in separate files to satisfy the size policy.

Register commands with stable names for preview, run, stop preview, and show
preview diagnostics. For Gradle projects, prefer the plugin task. For Maven
projects, prefer the Maven goal. For a converted project, continue to open the
resulting Gradle workspace after successful validation.

The wizard must continue to collect group ID, artifact ID, SDK version,
platforms, and any currently supported application settings. It generates a
Gradle project using the current official plugin ID. Do not turn it into a
generic CLI prompt.

The conversion command continues to parse the existing POM, create Gradle files
atomically, validate the wrapper, preserve the original POM under a unique backup
name, and restore prior files on ordinary failure. Shared pure functions may move
to a reusable TypeScript module, but the command and user messages remain in the
extension.

Add a preview view or use the Java AWT host according to the existing extension
design. If the extension displays frames itself, reuse the protocol rather than
screen scraping. Keep credentials out of session descriptors and logs.

## Surprises & Discoveries

- Observation: none recorded yet.
  Evidence: record only behavior that changes preservation or integration work.

## Decision Log

- Decision: the extension owns the wizard and conversion UX.
  Rationale: these are interactive editor workflows, not generic build mechanics.
  Date/Author: 2026-07-26 / User and OpenAI.

- Decision: isolate process/session access from VS Code UI classes.
  Rationale: it enables focused tests and keeps TypeScript files within limits.
  Date/Author: 2026-07-26 / OpenAI.

## Validation and Acceptance

Run existing extension tests first, then focused command tests using fake CLI
JSON streams. Acceptance requires a human-verifiable flow:

1. create a Gradle project through the wizard and open it;
2. convert a supported Maven project, validate it, and retain the POM backup;
3. start preview from VS Code;
4. edit one source file and observe a successful reload notification;
5. stop preview without leaving a worker process.

Also test a failed Gradle validation and prove rollback. Run TypeScript compile,
lint if configured, `git diff --check`, and the staged size checker.

## Risks and Open Questions

VS Code extension host shutdown can occur without command cleanup. Persist only
the session identifier and let Plan-08 stale-session cleanup handle crashes.
Avoid depending on a terminal shell or user-specific command quoting.

## Idempotence and Recovery

Project conversion must remain transactional. Re-running it on an already
converted project reports the state and does not overwrite unrelated Gradle
files. Starting preview twice attaches to or reports the existing session.
Extension deactivation attempts graceful detach but does not kill a session owned
by another client.

## Outcomes & Retrospective

Not started.

## Revision Note

2026-07-26: explicitly preserved the Gradle wizard and Maven conversion while
limiting shared tooling to reusable mechanics.

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
