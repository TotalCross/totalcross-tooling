# Port legacy preview capabilities to the canonical host/worker pipeline

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, `Outcomes & Retrospective`, and `Editorial Report` must remain current while the work proceeds. Maintain this file according to `vscode-extension/.agent/PLANS.md`.

## Purpose / Big Picture

The public `TotalCross: Preview` command must regain the useful editor integration that existed in the retired HTTP Live Preview without reviving that second runtime owner. While Preview is open, selecting a compiled Java class that extends `totalcross.ui.MainWindow`, `totalcross.ui.Container`, or `totalcross.ui.Control` should render that class in the VS Code panel. Invalid or unavailable targets should clear the panel without replacing a healthy worker. Closing the panel should stop the preview. Users should also be able to choose the project MainWindow, edit preview configuration, reload explicitly, and recover the panel after a VS Code window reload.

The result remains based on the authenticated process protocol in `tooling-java`; the legacy `live-preview-server` HTTP endpoints are not reactivated. Each completed milestone is committed separately with an English Conventional Commit subject and an explanatory body.

## Progress

- [x] (2026-08-25 19:36Z) Compared `origin/main_live_preview_original` with the active extension and inventoried capabilities that were disabled by commit `612e658`.
- [x] (2026-08-25 19:36Z) Read repository and nested instructions, inspected both relevant worktrees, and preserved the unrelated deploy edits and generated directories in `totalcross-tooling`.
- [x] (2026-08-25 19:48Z) Added the authenticated `SHOW`/`SHOW_READY` exchange, SDK compatibility bridge, candidate gating, CLI `show` command, selection result file, and focused Java coverage.
- [ ] Add authenticated worker protocol support for safely presenting `MainWindow`, `Container`, and `Control` targets, then connect it to the selected VS Code editor.
- [ ] Stop the complete preview session when its panel closes and add deterministic lifecycle coverage.
- [ ] Restore MainWindow discovery, project configuration, classpath overrides, and explicit configuration editing on the canonical preview path.
- [ ] Restore explicit reload and serialized panel revival without reintroducing the legacy HTTP service.
- [ ] Remove or clearly retire inactive legacy activation code, stale settings, tests, and documentation after every supported capability has a canonical owner.
- [ ] Run focused and broad validation, exercise the real sample when possible, and finalize the Editorial Report.

## Surprises & Discoveries

- Observation: The original branch is an ancestor of the current branch; the editor-selection behavior was merged and then deliberately removed from public activation during host/worker consolidation.
  Evidence: `0f447a5` introduced `onDidChangeActiveTextEditor`, `f3a3567` restored the original Live Preview, and `612e658` removed `activateLivePreview` plus its command contributions.

- Observation: The inactive legacy TypeScript remains compiled and its tests call `activateLivePreview` directly, so tests can pass even though production extension activation never registers those commands.
  Evidence: `vscode-extension/src/test/suite/extension.test.ts` invokes the legacy activation function instead of `extension.activate`.

- Observation: The canonical worker protocol can reload a whole preview target but cannot present a `Container` or `Control` inside an already initialized MainWindow.
  Evidence: `MessageType` and `ToolingCli.ControlLoop` expose start, reload, resize, pointer, key, and stop only. The SDK-backed `ReflectiveRuntimeBridge` exposes the narrow `tc.preview.PreviewSession` methods and has no show operation.

- Observation: Closing the current webview stops frame polling but retains the client, file watcher, coordinator, and worker.
  Evidence: `PreviewManager.startPreview` registers `panel.onDidDispose` with only `stopFramePolling`, whereas the legacy panel called `stopProcess`.

- Observation: The published preview contract does not expose presentation methods, but its simulator-backed session retains a private `Launcher` whose public compatibility methods match the retired server behavior.
  Evidence: `tc.preview.internal.SimulatorPreviewSession` owns `private final Launcher launcher`; `Launcher` exposes `preparePreviewMainWindowReload`, `replaceMainWindow`, `showContainer`, and `showControl`.

## Decision Log

- Decision: Port capabilities onto the authenticated host/worker and control-file pipeline instead of reactivating `live-preview-server`.
  Rationale: The repository records the HTTP server as legacy and the host/worker coordinator as the single supported reload owner. Restoring two owners would recreate lifecycle and packaging divergence.
  Date/Author: 2026-08-25 / Codex

- Decision: Present a selected class through a fresh worker candidate initialized with the configured MainWindow, and promote it only after the selected target produces a frame.
  Rationale: A fresh worker provides a fresh application classloader after compilation, while candidate promotion preserves the currently healthy preview if class construction or presentation fails.
  Date/Author: 2026-08-25 / Codex

- Decision: Keep configuration compatible with the existing `totalcross-preview.json` filename but define which fields the canonical path actually owns.
  Rationale: Conversion code already preserves this file. Reusing it avoids another migration format while allowing stale server-only fields to be retired explicitly.
  Date/Author: 2026-08-25 / Codex

- Decision: Append `SHOW` and `SHOW_READY` after every existing protocol enum value, and keep protocol version 1.
  Rationale: The host and worker are distributed as one tooling runtime, and appending preserves every existing ordinal on the wire. A version bump would reject otherwise compatible host/worker pairs without adding a negotiation path.
  Date/Author: 2026-08-25 / Codex

- Decision: Publish selection completion through `preview-selection.json` beside the frame file.
  Rationale: The previous active worker continues producing valid frames while a candidate is evaluated. An explicit class-scoped result lets the extension suppress those stale frames until the selected candidate succeeds and remain blank when selection fails.
  Date/Author: 2026-08-25 / Codex

## Outcomes & Retrospective

Implementation is in progress. The initial investigation established the capability gaps and the architectural constraint that all restored behavior must use the canonical host/worker lifecycle.

## Editorial Report

This report will be finalized from observed evidence when implementation is complete.

### Editorial Summary

Pending completion.

### Original Plan versus Actual Outcome

Pending completion.

### What Changed

Pending completion.

### Decisions and Trade-offs

Pending completion.

### Unexpected Problems and Discoveries

Pending completion.

### Validation and Measurable Results

Pending completion.

### Useful Evidence and Examples

Pending completion.

### Limitations, Remaining Work, and Open Questions

Pending completion.

### Possible Article Angles

Pending completion.

### Suggested Narrative

Pending completion.

### Claims Requiring Human Review

Pending completion.

## Context and Orientation

`vscode-extension/src/extension.ts` activates `PreviewManager` from `vscode-extension/src/preview-commands.ts`. The manager owns the webview panel, source watcher, frame polling, and `PreviewClient`. `PreviewClient` invokes the Gradle or Maven preview task and appends commands to `preview-control.txt`.

The build plugin starts `tooling-java/tooling-cli`, which owns a `PreviewReloadCoordinator`. A candidate is a separate Java worker represented by `tooling-java/preview-host/ProcessWorkerCandidate`. It is promoted only after authentication and a first valid frame. The worker loads application classes through a disposable URL classloader and binds to the SDK through `tooling-java/preview-worker/ReflectiveRuntimeBridge`.

The inactive `vscode-extension/src/live-preview.ts` contains the old editor listener, class-name discovery, MainWindow selection, configuration editor, panel serializer, and HTTP client. It is reference material only; supported behavior must move to the files above before the legacy activation path can be removed.

## Plan of Work

Milestone 1 adds a protocol message for presenting a class. `ReflectiveRuntimeBridge` will classify the selected class against SDK `MainWindow`, `Container`, and `Control` types, require a usable no-argument constructor, and invoke the simulator presentation operation. `ProcessWorkerCandidate` will optionally request the selected target and wait for the protocol acknowledgement plus a resulting frame before promotion. `ToolingCli` will accept a `show` control-file command that creates such a candidate while retaining the project MainWindow as the application root. The extension will debounce active-editor changes, derive the fully qualified Java class name, clear the webview immediately, and append `show,<class>`.

Milestone 2 makes webview ownership authoritative. Disposing the active panel will enqueue the same bounded stop path as `TotalCross: Stop Preview`, without recursively disposing a replacement panel or issuing duplicate stops. Tests will prove that the client and watcher are released.

Milestone 3 moves the relevant configuration behavior into the canonical manager. It will read or create `totalcross-preview.json`, discover MainWindow candidates, offer a Quick Pick when configuration does not identify one, and expose `Open Preview Config`. Canonical classpath and JVM overrides will be passed through supported Gradle and Maven properties or the extension environment; server-port settings will not be retained because the canonical preview has no HTTP listener.

Milestone 4 exposes explicit reload and registers a serializer for the canonical webview. Revival will reuse the serialized workspace identity and enter the manager lifecycle queue so it cannot overlap another start or stop. Reload will compile and re-present the active valid target, falling back to the configured MainWindow.

Milestone 5 removes inactive legacy activation code and updates tests, package contributions, README, changelog, and this plan. Any legacy file retained for conversion compatibility will be clearly scoped and must not advertise runtime behavior it no longer owns.

## Concrete Steps

From `tooling-java`, run focused protocol, host, worker, and CLI tests during Milestone 1:

    ./gradlew :tooling-protocol:test :preview-host:test :preview-worker:test :tooling-cli:test --console=plain

From `vscode-extension`, compile and run integration tests after every extension milestone:

    npm run compile
    npm test

When Gradle or Maven plugin command construction changes, stage tooling modules under a unique temporary version and run the plugin suites without replacing unrelated local Maven artifacts. At the end, run:

    python3 vscode-extension/tools/check-repository-governance.py
    python3 -m unittest discover -s vscode-extension/tests -p 'test_repository_governance.py'
    git diff --check

## Validation and Acceptance

Automated acceptance requires a real protocol candidate test proving that a selected target is requested before promotion and that a target failure preserves the previous worker. Worker tests must prove classification for MainWindow, Container, Control, incompatible classes, and missing default constructors without linking tooling at compile time to SDK classes.

Extension tests must prove that selecting a Java editor while Preview is active emits one debounced `show` command with its fully qualified class, clears the previous frame while selection is pending, and repeats after a successful build of the active class. Closing the panel must await session shutdown. Configuration tests must prove deterministic MainWindow selection and preservation of user fields. Serializer tests must prove that revival and explicit reload use the manager lifecycle queue.

Manual acceptance uses a compatible TotalCross sample: start Preview, switch among compiled Container and Control sources, observe the corresponding UI, select an incompatible source and observe a blank panel, edit the active class and observe its new compiled form, close the panel and verify no preview PID remains, then reload VS Code and verify a serialized panel can restart its project preview.

## Idempotence and Recovery

Tests and compilation commands are safe to repeat. New process tests must terminate workers in `finally` or try-with-resources blocks. Do not clean the pre-existing generated `tooling-java` directories or touch the unrelated deploy source and tests. Stage every commit with explicit paths and inspect `git diff --cached` before committing.

If a protocol experiment cannot present classes through the published SDK contract, retain the last passing commit, record the limitation in this plan, and use a narrowly documented compatibility bridge rather than reactivating the HTTP server. Do not modify or commit the adjacent `totalcross` repository unless the scope is explicitly expanded and its clean branch base is verified.

## Artifacts and Notes

The initial worktree has user-owned changes in `tooling-java/tooling-core/src/main/java/com/totalcross/tooling/deploy/LegacyDeployService.java`, an untracked deploy test package, and generated Gradle/build directories. These paths are out of scope and must remain unstaged.

The historical evidence file `.agent/evidence/unified-tooling-preview.md` states that the old HTTP/Webview commands were left testable but intentionally removed from public activation in 2026-07-28.

## Interfaces and Dependencies

Protocol message additions must be versioned through `tooling-java/tooling-protocol/MessageType` and handled symmetrically by `PreviewHost`, `PreviewWorkerSession`, and their tests. The extension-to-coordinator control file remains line-oriented and comma-separated, so class names must be validated as Java fully qualified names before writing.

The compatibility bridge may reflect on the SDK simulator implementation because released SDKs do not yet expose show operations in `tc.preview.PreviewSession`. It must fail with a clear diagnostic when the session is not simulator-backed, and the limitation must be recorded for future SDK contract work.

Revision note (2026-08-25 19:36Z): Created after comparing the retired HTTP Live Preview with the canonical host/worker path and selecting an incremental, commit-by-commit port strategy.

Revision note (2026-08-25 19:48Z): Recorded the implemented worker-selection protocol, published-SDK compatibility bridge, selection result file, and passing focused Java tests.
