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

- [x] Measure launcher responsibilities and existing tests.
- [x] Add characterization tests for desktop launch and current preview behavior.
- [x] Split `Launcher.java` into focused collaborators.
- [x] Keep the public `totalcross.Launcher` facade compatible.
- [x] Add a runtime-neutral frame and command adapter.
- [x] Preserve the existing AWT compatibility surface.
- [x] Run focused desktop and preview validation.
- [x] Commit and update state to Plan 05.

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

Completed on 2026-07-26. The desktop launcher is now a small public facade over
package-private responsibility layers for state, arguments, input, rendering,
storage, settings, fonts, and stream/window compatibility. The original AWT
`PreviewRuntime.FrameConsumer` remains available, while `PreviewFrame` and
`PreviewFrameConsumer` provide copied pixels, dimensions, stride, density, and
format without exposing AWT or TotalCross UI objects. `PreviewCommandAdapter`
defines start, pump, resize, pointer, key, reload, replacement, and close
commands with structured lifecycle events for the next plan.

## Revision Note

2026-07-26: added the mandatory launcher decomposition and separated runtime
frame semantics from the future wire protocol.

## Editorial Report

This section is mandatory at completion. Keep it factual and evidence-based.

### Editorial Summary

The extraction followed the intended boundary and kept the public facade. The
existing AWT path still aliases its image buffer for compatibility; the new
frame path copies pixels before delivery. The command adapter is deliberately
an in-process neutral boundary; Plan 05 owns its worker and transport binding.

### Original Plan versus Actual Outcome

`Launcher.java` dropped below the file-size policy after responsibility layers
were introduced. The SDK gained `PreviewFrame`, `PreviewFrameConsumer`,
`PreviewLifecycleEvent`, and `PreviewCommandAdapter`; focused tests cover frame
ownership, command forwarding, lifecycle ordering, and legacy AWT presentation.

### What Changed

The compatibility constructor remains unchanged for existing callers. A
separate frame-consumer setter and `LauncherRuntime.startPreviewFrames` avoid
ambiguous overloads with existing `null`-accepting constructors.

### Decisions and Trade-offs

The initial mechanical extraction exposed inherited-field and nested-type
compatibility assumptions, including reflective access to `toScale`/`toBpp`
and `Launcher.UserFont`; narrow facade compatibility shims preserved those
behaviors without restoring the oversized implementation.

### Unexpected Problems and Discoveries

The focused Gradle-agent test run passed 5 tests, including the existing
launcher/parser/runtime tests plus `PreviewFrameTest` and
`PreviewCommandAdapterTest`. `git diff --check` passed; all touched launcher
and preview source/test files are below 20 KiB and 600 lines.

### Validation and Measurable Results

Evidence: `/tmp/totalcross-plan04-preview-test.log` and the corresponding SDK
agent log `TotalCrossSDK/agent-logs/20260726-190501-test-full.log`.

### Useful Evidence and Examples

No standalone worker or process protocol exists yet; those remain Plan 05.
The focused tests do not launch a user-supplied minimal MainWindow, so full
application lifecycle validation remains part of later integration checks.

### Limitations, Remaining Work, and Open Questions

Launcher decomposition can be explained as preserving the old desktop surface
while making frame ownership explicit before a process boundary is introduced.

### Possible Article Angles

Start with the oversized launcher, show the compatibility-preserving split,
then demonstrate why copied frames and structured commands are prerequisites
for an IDE preview worker.

### Suggested Narrative

Claims about cross-process security, worker supervision, and protocol behavior
require review after Plan 05; this plan only defines the in-process neutral
command boundary.

### Claims Requiring Human Review

Review that the facade shims are acceptable long-term before later plans widen
the public preview API; no external protocol claims are made here.
