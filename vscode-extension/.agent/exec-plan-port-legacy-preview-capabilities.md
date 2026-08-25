# Port legacy preview capabilities to the canonical host/worker pipeline

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, `Outcomes & Retrospective`, and `Editorial Report` must remain current while the work proceeds. Maintain this file according to `vscode-extension/.agent/PLANS.md`.

## Purpose / Big Picture

The public `TotalCross: Preview` command must regain the useful editor integration that existed in the retired HTTP Live Preview without reviving that second runtime owner. While Preview is open, selecting a compiled Java class that extends `totalcross.ui.MainWindow`, `totalcross.ui.Container`, or `totalcross.ui.Control` should render that class in the VS Code panel. Invalid or unavailable targets should clear the panel without replacing a healthy worker. Closing the panel should stop the preview. Users should also be able to choose the project MainWindow, edit preview configuration, reload explicitly, and recover the panel after a VS Code window reload.

The result remains based on the authenticated process protocol in `tooling-java`; the legacy `live-preview-server` HTTP endpoints are not reactivated. Each completed milestone is committed separately with an English Conventional Commit subject and an explanatory body.

## Current Stop Point

Implementation was deliberately paused at the user's request after the Java-side selection foundation was completed. Commit `7a2b8d7` adds the authenticated selection protocol and SDK compatibility bridge; commit `894d7ca` serializes frame promotion and writes the selected frame before its result marker. No partial TypeScript editor-selection implementation remains in the worktree.

The next implementer should start with Milestone 1 below. The only expected dirty paths at this checkpoint are the maintainer-owned `tooling-java/tooling-core/src/main/java/com/totalcross/tooling/deploy/LegacyDeployService.java`, its untracked deploy tests, and generated `tooling-java` build directories. Do not stage or remove them.

## Progress

- [x] (2026-08-25 19:36Z) Compared `origin/main_live_preview_original` with the active extension and inventoried capabilities that were disabled by commit `612e658`.
- [x] (2026-08-25 19:36Z) Read repository and nested instructions, inspected both relevant worktrees, and preserved the unrelated deploy edits and generated directories in `totalcross-tooling`.
- [x] (2026-08-25 19:48Z) Added the authenticated `SHOW`/`SHOW_READY` exchange, SDK compatibility bridge, candidate gating, CLI `show` command, selection result file, and focused Java coverage.
- [x] (2026-08-25 19:52Z) Serialized frame consumption with candidate promotion so a successful selection marker always follows the corresponding PNG.
- [x] (2026-08-25 20:03Z) Stopped implementation on request, removed the uncommitted TypeScript draft, and converted this document into the handoff plan for the remaining work.
- [x] (2026-08-25 20:05Z) Connected the selected Java editor to the committed `show` control command, authoritative model class-output path, and class-scoped selection-result marker; added debounce, blanking, and stale-frame suppression.
- [ ] Add deterministic lifecycle coverage for editor selection and re-presentation after a build.
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

- Observation: Frame polling and control-file reloads run on different CLI threads, so the main loop could retain the previous candidate while the control thread promoted and closed it.
  Evidence: `PreviewReloadCoordinator.nextFrame` copied the active candidate before polling it, while `ControlLoop` called synchronized `reload` independently. The CLI now serializes candidate promotion and frame consumption and writes a selected frame before publishing success.

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

The authenticated protocol, disposable selected candidates, reflective SDK presentation, candidate/frame ordering, and focused Java tests are complete. The extension now reads the generated project model, follows the active Java editor with a debounced `show` request, blanks while a matching selection marker is pending or failed, and suppresses stale frame posts. Panel lifecycle ownership, canonical configuration, explicit reload, revival, and legacy retirement remain planned below.

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

### Milestone 1: follow the active Java editor

Add a small canonical TypeScript module, separate from `live-preview-utils.ts`, for package/class extraction, workspace containment, compiled-class resolution, and parsing `preview-selection.json`. Extend `PreviewClient` with a typed project-model reader that exposes `mainClass` and `classOutput`; do not assume Gradle's default output when the generated model already provides the authoritative path.

The build-tool launcher currently returns before the coordinator is ready. Expose a readiness promise from `PreviewClient` that resolves only when the Gradle or Maven invocation exits successfully. Both plugins already wait for the coordinator's first frame before they return, so controls written after this promise resolves cannot be mistaken for stale control-file lines. Preserve the existing non-blocking `start`/concurrent `stop` behavior and add a rejection path for a failed launcher.

While Preview is active, subscribe to `vscode.window.onDidChangeActiveTextEditor`. Cancel the prior 150 ms debounce on every editor change, but send a request only for a Java file inside the selected project. Derive the fully qualified top-level class name from the document text, verify its `.class` under the model's `classOutput`, clear the webview, remove an obsolete selection result, and append `show,<class>`. Track the pending class and suppress frame polling until `preview-selection.json` reports that same class. On success, resume frames; on failure or an unavailable class, remain blank and write a diagnostic without replacing the healthy worker.

After a watcher-triggered successful build, force the active Java class through `show` again so edits update the selected preview. If the active editor is not a project Java source, keep the existing MainWindow reload behavior. Ensure overlapping polling callbacks cannot post an old frame after a clear.

Tests must cover source/FQN extraction, model-provided output paths, workspace filtering, debounce cancellation, one `show` command per selection, blanking for missing classes, class-scoped success/failure markers, stale-marker rejection, and re-presentation after build. Commit as:

    feat(vscode): follow the active preview editor

### Milestone 2: make panel disposal own shutdown

Treat the current `WebviewPanel` as the owner of the active preview session. Its disposal callback must clear the panel reference first and enqueue `stopPreview` with panel disposal disabled, preventing recursive disposal. Manual stop, extension deactivation, start replacement, and panel close must converge on the same idempotent lifecycle promise.

Dispose the source watcher, editor listener, timers, and output polling before awaiting `PreviewClient.stop`. Add tests proving a panel close requests exactly one stop, two concurrent stop paths share completion, and all owned disposables are released. Commit as:

    fix(vscode): stop preview when its panel closes

### Milestone 3: restore canonical MainWindow configuration

Define a typed, version-tolerant preview configuration in `tooling-core` and keep the existing root filename `totalcross-preview.json`. Canonical fields are `mainWindow`, `launcherArgs`, and additional classpath entries. For compatibility, read legacy `classOutputPaths`, `resourcePaths`, and `dependencyPaths` as additional classpath inputs, but keep the generated `project-model.json` entries authoritative and deduplicated. Do not revive `port`, `previewMode`, `headlessOutput`, `buildCommand`, or HTTP control fields.

Pass the configuration path from both Gradle and Maven preview launchers to `ToolingCli`. The CLI should merge classpath overrides before creating workers and use the configured MainWindow and launcher arguments for initial candidates. Replace the fixed `initialMainClass` captured by the committed `show` handler with mutable session-root state that changes only after a successful explicit MainWindow reload; selecting another MainWindow as an editor target must not silently rewrite project configuration.

Port MainWindow source discovery into the canonical extension. Prefer the configured class when valid, otherwise prefer a class referenced by `TotalCrossApplication.run`, automatically select the only candidate, or show a Quick Pick for ambiguity. Add `TotalCross: Select Preview MainWindow` and `TotalCross: Open Preview Config`; write config atomically and preserve unknown user fields. Forward supported `totalcross.livePreview.jvmArgs` through the existing headless `JAVA_TOOL_OPTIONS` construction. Either map `deviceProfile` to documented launcher arguments or remove it in Milestone 5; do not leave it inert.

Split this milestone into two buildable commits if needed:

    feat(preview): load canonical preview configuration
    feat(vscode): restore preview MainWindow selection

Tests must cover Gradle and Maven command parity, classpath ordering/deduplication, configured MainWindow startup, mutable session-root behavior, deterministic discovery, ambiguous Quick Pick input, atomic config preservation, and paths containing spaces.

### Milestone 4: explicit reload and panel revival

Contribute `TotalCross: Reload Preview`. When a session is active, enqueue a build and re-present the active valid Java target; otherwise reload the configured MainWindow. Do not start a new independent manager or bypass the lifecycle queue.

Register a serializer for `totalcrossPreview`. The webview must persist only the workspace-folder URI and presentation state required for recovery. Deserialization must reuse the same manager, existing panel, and lifecycle queue, then start a fresh canonical preview session for that workspace. Add the `onWebviewPanel:totalcrossPreview` activation event if required by the supported VS Code engine.

Use separate commits because reload is independently testable:

    feat(vscode): add explicit preview reload
    feat(vscode): revive preview panels after reload

Tests must cover queued reload versus stop, fallback to the configured MainWindow, revival of the correct multi-root workspace, failed revival cleanup, and absence of duplicate panels or processes.

### Milestone 5: retire inactive legacy surfaces and document the result

Once every retained capability has a canonical owner, remove the inactive `activateLivePreview` runtime and replace tests that call it directly with production `extension.activate` coverage. Remove HTTP-only command contributions and settings (`javaCommand`, `port`, and `controlTimeout`) plus any other setting proven unused. Keep `live-preview-server` only as a clearly labeled historical compatibility module unless repository scope explicitly authorizes deletion; it must not be packaged or advertised as the VS Code runtime.

Update README, CHANGELOG, package metadata, evidence, and this ExecPlan. Document headless execution, active-editor type/constructor requirements, behavior for uncompiled or incompatible classes, configuration ownership, reload, revival, and panel-close shutdown. Commit code retirement separately from documentation:

    refactor(vscode): retire the legacy preview runtime
    docs(preview): document the restored editor workflow

## Concrete Steps

From `tooling-java`, run focused protocol, host, worker, and CLI tests during Milestone 1:

    ./gradlew :tooling-protocol:test :preview-host:test :preview-worker:test :tooling-cli:test --console=plain

The committed backend baseline already passed these focused tasks after `894d7ca`. Re-run them whenever the CLI session root or configuration/classpath logic changes.

From `vscode-extension`, compile and run integration tests after every extension milestone:

    npm run compile
    npm test

When Gradle or Maven plugin command construction changes, stage tooling modules under a unique temporary version and run the plugin suites without replacing unrelated local Maven artifacts. At the end, run:

    python3 vscode-extension/tools/check-repository-governance.py
    python3 -m unittest discover -s vscode-extension/tests -p 'test_repository_governance.py'
    git diff --check

Before every commit, use explicit `git add` paths and confirm the cached diff excludes `LegacyDeployService.java`, the untracked deploy tests, and generated build directories. Every commit subject and body must be in English, use Conventional Commits, and leave its touched modules buildable.

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

Revision note (2026-08-25 19:52Z): Recorded and closed the cross-thread frame/promotion race discovered while designing stale-frame suppression in the extension.

Revision note (2026-08-25 20:03Z): Implementation was paused at the user's request. Removed the uncommitted TypeScript draft and expanded the remaining milestones into an executable commit-by-commit handoff plan.
