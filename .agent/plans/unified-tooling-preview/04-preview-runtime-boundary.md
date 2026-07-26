<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Decompose the desktop launcher and define the preview runtime boundary

This ExecPlan is Plan 04. It changes the branch-392 preview implementation without
moving converter or native IR source.

## Purpose / Big Picture

Reduce the oversized desktop launcher into focused classes, preserve current
desktop behavior, and expose a preview runtime adapter that tooling can use
without placing AWT images or TotalCross UI objects in the external protocol.

## Working Set and Resume Protocol

Read state and this plan. Inspect only:

    TotalCrossSDK/src/main/java/totalcross/Launcher.java
    TotalCrossSDK/src/main/java/totalcross/LauncherRuntime.java
    TotalCrossSDK/src/main/java/totalcross/preview/**
    focused launcher and preview tests

Use `git diff --stat` before opening a large diff. Do not inspect converter or
TCIR paths.

## Progress

- [ ] Measure launcher responsibilities and existing tests.
- [ ] Add characterization tests for desktop launch and current preview behavior.
- [ ] Split `Launcher.java` into focused collaborators.
- [ ] Keep the public `totalcross.Launcher` facade compatible.
- [ ] Add a runtime-neutral frame and command adapter.
- [ ] Preserve the existing AWT compatibility surface.
- [ ] Run focused desktop and preview validation.
- [ ] Commit and update state to Plan 05.

## Current Architecture and Scope

`Launcher.java` currently combines argument parsing, settings, AWT events,
rendering, application lifecycle, timer coordination, class loading, and preview
behavior. Because it is not in a protected IR path and will be modified, it must
be brought below the 20 KiB/600-line policy.

Create collaborators under `totalcross.launcher` or
`totalcross.preview.internal`, using names that match responsibilities. A
reasonable target is:

    LauncherArguments
    LauncherSettings
    LauncherLifecycle
    LauncherEventBridge
    LauncherRenderer
    LauncherWindowController
    LauncherAttachedFiles

Do not split merely by line ranges. Move cohesive behavior and keep package
visibility where it avoids expanding public API. Each class must have focused
tests where practical.

The current `PreviewRuntime` may remain as a deprecated compatibility interface.
Add runtime-side types that represent copied frame pixels, dimensions, density,
lifecycle, resize, and input without defining the cross-process wire format.
The tooling protocol is created in Plan 05.


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

First add characterization tests that start a minimal `MainWindow`, verify
argument parsing, create at least one rendered frame, close the runtime, and
exercise current preview reload preparation. These tests protect behavior during
the split.

Refactor one responsibility per commit or tightly related commit pair. After
each move, run only the affected test. Keep `Launcher` as the public facade and
delegate to collaborators. Preserve synchronization through the existing UI
lock; do not move synchronization casually across callbacks.

Extract rendering so a frame can be copied into a small immutable runtime frame
object containing width, height, stride, pixel format, and an owned pixel array.
Do not retain launcher-owned backing arrays after the callback. Keep an adapter
that still supplies `BufferedImage` to existing branch-392 callers.

Define runtime command methods for start, pump, resize, pointer, key, reload
preparation, main-window replacement, and close. Do not expose `Container`,
`Control`, or `MainWindow` across the future process boundary. Internal overloads
may still use them inside the SDK.

Add lifecycle diagnostics for start, ready, frame, reload-ready, error, and
closed. Keep diagnostics structured enough for Plan 05 to translate without
parsing stack-trace text.

## Surprises & Discoveries

- Observation: none recorded yet.
  Evidence: add only behavior that materially changes the extraction design.

## Decision Log

- Decision: split Launcher before adding more preview behavior.
  Rationale: the user requires every modified non-protected file to meet the
  size policy, and smaller responsibilities reduce reload risk.
  Date/Author: 2026-07-26 / OpenAI.

- Decision: keep an AWT compatibility adapter but make copied pixels the stable
  runtime concept.
  Rationale: current callers keep working while the external protocol avoids AWT.
  Date/Author: 2026-07-26 / OpenAI.

## Validation and Acceptance

Focused tests must prove that the legacy desktop launcher still starts and
closes, the compatibility preview receives a frame, and the new copied-frame
adapter receives equivalent dimensions and pixels. Add a test that mutating or
reusing the launcher buffer does not change a retained copied frame.

Run the SDK wrapper with the smallest relevant task and keep full output in its
agent log. Do not run a full distribution build unless the launcher split
changes packaging. The staged size checker must confirm every modified
non-protected file is within limits.

## Risks and Open Questions

Synchronization and lifecycle order are the main risks. If characterization
reveals behavior that depends on `Launcher` field access from other packages,
introduce narrow package-private accessors rather than making collaborators
public. Record any unavoidable compatibility field.

## Idempotence and Recovery

Keep each extraction behavior-preserving and separately revertible. Do not mix a
large move with protocol behavior changes. If a test fails after extraction,
restore delegation for that responsibility without reverting unrelated splits.

## Outcomes & Retrospective

Not started.

## Revision Note

2026-07-26: added the mandatory launcher decomposition and separated runtime
frame semantics from the future wire protocol.

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
