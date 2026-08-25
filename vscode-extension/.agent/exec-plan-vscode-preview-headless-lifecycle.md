# Make VS Code preview headless and reliably stoppable

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, `Outcomes & Retrospective`, and `Editorial Report` must be kept up to date as work proceeds. Maintain this document in accordance with `vscode-extension/.agent/PLANS.md`.

## Purpose / Big Picture

The VS Code preview must stop every Java coordinator and worker that it starts, including when Stop Preview is requested during startup or when Preview is started a second time. After this change, the Preview command renders only in the VS Code webview and its Java processes explicitly run with `java.awt.headless=true`; the separate Run command remains graphical. A developer can observe the result by starting Preview, seeing frames in the webview without a Java desktop window, stopping it, and confirming that the recorded coordinator process and its worker are no longer alive.

## Progress

- [x] (2026-08-25 04:51Z) Read repository instructions, inspected the dirty worktree, and preserved the unrelated Java deploy changes and generated build directories.
- [x] (2026-08-25 04:51Z) Traced Preview and Stop Preview from `vscode-extension` through the Gradle and Maven plugins into the shared Java coordinator and worker.
- [x] (2026-08-25 05:04Z) Added regression tests for serialized VS Code start/stop behavior, descendant process termination, and explicit preview-only headless JVM arguments.
- [x] (2026-08-25 05:04Z) Implemented lifecycle serialization in the VS Code preview client and manager.
- [x] (2026-08-25 05:04Z) Made Gradle and Maven preview coordinator shutdown cooperative-first, bounded, and forceful as a fallback through a shared tooling-core terminator.
- [x] (2026-08-25 05:04Z) Passed headless mode to both the preview coordinator and preview worker while preserving graphical Java commands for the build-tool Run mode.
- [x] (2026-08-25 05:04Z) Ran focused and broad TypeScript, Java, Gradle, Maven, governance, and runtime-dependency checks.
- [x] (2026-08-25 05:04Z) Reconciled validation evidence into the retrospective and finalized the Editorial Report.
- [x] (2026-08-25 19:07Z) Reproduced the reported Java window with the real `tc-sample` project and established that it resolved the published `0.1.0-beta.1` Java tooling, so the Java-source headless changes were not present at runtime.
- [x] (2026-08-25 19:07Z) Made the extension propagate preview-only headless options through `JAVA_TOOL_OPTIONS`, added focused regression coverage, and validated frame generation without a registered Java application window on macOS.
- [x] (2026-08-25 19:07Z) Re-ran the extension integration suite and reconciled the new runtime evidence into this plan and Editorial Report.

## Surprises & Discoveries

- Observation: `PreviewManager.start()` disposes only the previous webview panel and then overwrites `this.client`; it does not stop the previous preview client or watcher.
  Evidence: `vscode-extension/src/preview-commands.ts` calls `disposePanel()` before assigning a new `PreviewClient`, while `stop()` is the only path that invokes `client.stop()`.

- Observation: `PreviewClient.start()` returns immediately after spawning Gradle or Maven, while `stop()` launches the build-tool stop command without awaiting it and clears the local process reference immediately.
  Evidence: `vscode-extension/src/preview-client.ts` assigns `spawn(...)` and returns; `stop()` uses `void this.run(command).catch(...)`.

- Observation: both plugin stop implementations send only `ProcessHandle.destroy()` to descendants and the coordinator, then delete the session descriptor without waiting or escalating to `destroyForcibly()`.
  Evidence: `gradle-plugin/.../TotalCrossPreviewStopTask.java` and `maven-plugin/.../TotalCrossPreviewStopMojo.java` contain the same unbounded best-effort termination sequence.

- Observation: the Java CLI avoids constructing `AwtPreviewWindow` in `preview` mode, but neither the coordinator JVM nor its separately spawned worker JVM sets `java.awt.headless=true`.
  Evidence: `tooling-java/tooling-cli/.../ToolingCli.java` selects the window from the `run` command, and `ProcessWorkerCandidate` receives a worker command without a headless system property.

- Observation: the Gradle plugin initially resolved an older `0.1.0-SNAPSHOT` tooling-core artifact from `mavenLocal()` before the isolated staging repository, so its first compile could not see `PreviewProcessTerminator`.
  Evidence: the first Gradle plugin test failed with `package com.totalcross.tooling.preview does not exist`; republishing as the unique temporary version `0.1.0-preview-fix` and passing that coordinate made the full 19-test suite succeed.

- Observation: the committed full npm audit currently reports two high-severity development-only dependency findings in `brace-expansion` and `js-yaml`; the production dependency audit is clean.
  Evidence: `npm run audit` exited 1 with the two advisories, while `npm audit --omit=dev --audit-level=low` reported `found 0 vulnerabilities`. No dependency update was mixed into this preview lifecycle change.

- Observation: changing Java command construction in the repository was insufficient for an installed extension whose project still resolved published `0.1.0-beta.1` plugin and tooling artifacts.
  Evidence: the live `tc-sample` coordinator and worker command lines both referenced `tooling-*-0.1.0-beta.1.jar` and neither JVM had `java.awt.headless` before the extension-level propagation change.

- Observation: propagating headless settings from the build-tool launcher reaches detached coordinator and worker processes even when the published plugin does not add those command-line flags itself.
  Evidence: launching `tc-sample` with `JAVA_TOOL_OPTIONS='-Djava.awt.headless=true -Dapple.awt.UIElement=true'` produced the first frame; `jcmd` reported both properties on worker PID 26641, and the macOS application list no longer contained `PreviewWorkerMain` or another Java application.

## Decision Log

- Decision: Treat Stop Preview as an asynchronous operation that must be serialized with an in-flight start, and make a repeated Preview stop the old session before replacing it.
  Rationale: this closes both lifecycle races at the VS Code ownership boundary without changing the public command set.
  Date/Author: 2026-08-25 / Codex

- Decision: Keep the plugin stop tasks as the durable fallback, but first request cooperative shutdown through `preview-control.txt`, wait for a bounded interval, and forcibly terminate any remaining process tree before removing its descriptor.
  Rationale: cooperative shutdown lets the coordinator close its worker and runtime cleanly; a time bound and force fallback prevent a hung Java UI thread from becoming an orphan.
  Date/Author: 2026-08-25 / Codex

- Decision: Set `-Djava.awt.headless=true` only for `preview` coordinator and worker JVMs, not for `run`.
  Rationale: Preview is rendered in the VS Code webview, while Run intentionally owns an AWT desktop window and must remain graphical.
  Date/Author: 2026-08-25 / Codex

- Decision: Also set preview-specific `JAVA_TOOL_OPTIONS` on the build launcher spawned by `PreviewClient`, adding `-Dapple.awt.UIElement=true` on macOS and preserving existing environment options.
  Rationale: the extension must suppress the window when a project resolves an older published plugin; environment inheritance crosses the build tool, detached coordinator, and worker boundaries without changing the sample project or relying on unreleased Java artifacts.
  Date/Author: 2026-08-25 / Codex

## Outcomes & Retrospective

The VS Code manager now serializes all Preview and Stop Preview operations. Starting a replacement preview first completes cleanup of the owned session, and stopping during startup waits for the build-tool launcher to finish writing the coordinator PID before invoking the stop task. The client no longer silently discards stop failures.

Gradle and Maven now delegate stop semantics to `PreviewProcessTerminator`. The terminator snapshots coordinator descendants, appends `stop` to the control file, waits two seconds, escalates to normal termination, then to forced termination, and removes the descriptor only after the captured tree is dead. A regression test proved the forced path against a Java parent with a blocking shutdown hook and a sleeping child; both PIDs stopped.

Preview coordinator and worker commands now carry `-Djava.awt.headless=true`. The extension additionally propagates headless options from its preview build launcher, covering projects that still resolve older published tooling. Run-mode coordinator and worker command tests prove that direct build-tool Run remains graphical. Automated validation passed across the extension and all affected Java modules. A real SDK-backed `tc-sample` launch generated its first frame with both headless properties present on the worker and without registering a Java application window on macOS; Windows and Linux still need release testing.

## Editorial Report

This report describes the completed implementation and observed validation as of 2026-08-25 19:07Z.

### Editorial Summary

The VS Code preview lifecycle could lose ownership of a Java preview process during overlapping start and stop operations. A second Preview could replace the only client reference, and Stop Preview could run before a starting build task had recorded the detached coordinator PID. Even after finding the PID, the plugin stop tasks issued best-effort termination and deleted the only recovery descriptor immediately.

The completed implementation serializes lifecycle operations, waits for launcher startup to settle before stop, and shares a bounded coordinator/worker tree terminator between Gradle and Maven. Preview coordinator and worker JVMs are explicitly headless, while build-tool Run mode remains graphical. The extension also propagates headless options through the build launcher so the behavior does not depend on a project already using newly built Java tooling.

### Original Plan versus Actual Outcome

The original plan intended lifecycle serialization, cooperative and forced process cleanup, and preview-only headless JVMs. All three were implemented. The process termination logic was placed in a shared tooling-core class rather than duplicated inside the two plugins; this reduced behavioral drift and enabled one realistic descendant-process regression suite. A later real-project reproduction showed that Java-source command changes alone do not affect projects resolving the published beta tooling, so extension-level environment propagation was added and verified against `tc-sample`. The unrelated npm development dependency advisories were recorded rather than repaired as part of this focused change.

### What Changed

`vscode-extension/src/preview-client.ts` now tracks launcher completion, makes stop awaitable, serializes it with startup, exposes a narrow process-spawner seam for deterministic tests, and supplies a copied preview environment containing `-Djava.awt.headless=true` plus the macOS UI-element setting. `vscode-extension/src/preview-commands.ts` serializes manager lifecycle operations and disposes timers, watchers, panels, and clients in ownership order.

`tooling-java/tooling-core/src/main/java/com/totalcross/tooling/preview/PreviewProcessTerminator.java` implements descriptor-driven cooperative, normal, and forced tree shutdown. The Gradle and Maven stop tasks delegate to it. `TotalCrossPreviewTask`, `TotalCrossPreviewMojo`, and `ToolingCli` add headless JVM arguments only for Preview coordinator and worker commands. Tests and README/changelog text were updated in each affected module.

### Decisions and Trade-offs

Preview is explicitly headless at both Java process layers because JVM system properties are not automatically copied into separately spawned JVM command lines. The extension additionally uses `JAVA_TOOL_OPTIONS`, which Java launchers in the preview process tree inherit, to cover published plugin versions that predate the command changes. Existing Java options are preserved and the extension's required values are appended; `-Dapple.awt.UIElement=true` prevents a macOS application presence if native UI initialization occurs. Direct build-tool Run remains without these preview-only additions so `AwtPreviewWindow` can still open. Stop favors the existing authenticated control protocol because it closes the worker runtime cleanly, but it retains bounded OS-level termination because a blocked UI or shutdown thread cannot be trusted to cooperate indefinitely. The session descriptor is retained when forced termination still fails, preserving a PID for diagnosis and recovery.

### Unexpected Problems and Discoveries

The lifecycle race spans VS Code, a build-tool launcher, a detached coordinator, and a worker subprocess; fixing only the webview or only the original build-tool child would not prove that the Java tree terminates. A separate build validation surprise was dependency precedence: `mavenLocal()` masked the newly staged snapshot, so a unique temporary tooling version was required to test the plugins without mutating the user's local Maven repository. The real-project reproduction exposed a second artifact-boundary issue: the extension source was updated, but `tc-sample` continued to load the published `0.1.0-beta.1` Java JARs. Moving the guarantee to the extension-owned launcher made the fix effective across that version boundary.

### Validation and Measurable Results

Observed results:

- `npm test` in `vscode-extension` after extension-level propagation: exit 0, 42 passing and 1 conditional VSIX test pending.
- `./gradlew test` in `tooling-java`: exit 0, 71 tests across the reported module XML files.
- `./gradlew test -PtoolingVersion=0.1.0-preview-fix -PtoolingRepository=file:///tmp/totalcross-preview-staging.MSiDbc --refresh-dependencies` in `gradle-plugin`: exit 0, 19 tests.
- `mvn -Prelease-staging -Dtooling.version=0.1.0-preview-fix -Dtooling.repository=file:///tmp/totalcross-preview-staging.MSiDbc test` in `maven-plugin`: exit 0, 3 tests.
- `python3 tools/check-repository-governance.py`: exit 0, governance baseline valid.
- `python3 -m unittest tests.test_repository_governance`: exit 0, 17 tests.
- `git diff --check`: exit 0 with no output.
- `npm audit --omit=dev --audit-level=low`: exit 0, zero production vulnerabilities. `npm run audit` remains nonzero because of two development-only findings described under limitations.
- A post-test process listing found no surviving `PreviewProcessTerminatorTest` or `ToolingCli preview/run` processes.
- `JAVA_TOOL_OPTIONS='-Djava.awt.headless=true -Dapple.awt.UIElement=true' ./gradlew totalcrossPreview --console=plain` in `tc-sample`: exit 0 after the first frame, using published `0.1.0-beta.1` tooling. `jcmd` reported both properties on the worker, and Computer Use listed no running Java or `PreviewWorkerMain` application. `totalcrossPreviewStop` then removed both coordinator and worker processes.

### Useful Evidence and Examples

The core evidence is the ordered lifecycle and `previewEnvironment` helper in `vscode-extension/src/preview-client.ts`, the ownership rules in `vscode-extension/src/preview-commands.ts`, the bounded algorithm in `tooling-java/tooling-core/src/main/java/com/totalcross/tooling/preview/PreviewProcessTerminator.java`, and the parent-plus-child forced-shutdown fixture in its matching test. Command tests in the Gradle plugin, Maven plugin, and tooling CLI show the preview/run headless distinction directly. The `tc-sample` first-frame output, worker system properties, and macOS application-list check provide runtime evidence across the published-plugin boundary.

### Limitations, Remaining Work, and Open Questions

A real SDK-backed sample was validated on macOS, but Windows and Linux window-system and process semantics remain release-test responsibilities. The runtime check invoked the same published Gradle preview task with the environment that the extension now supplies; a packaged VSIX was not manually driven through its UI during that check. `npm run audit` reports two high-severity findings confined to development dependencies (`brace-expansion` and `js-yaml`); updating those dependencies is separate work because it changes the committed dependency graph beyond this preview fix.

### Possible Article Angles

- For extension authors: “Stopping the process you spawned is not enough: managing a build-tool coordinator and worker tree.” The takeaway is how to maintain ownership across detached subprocess layers.
- For Java tooling maintainers: “Keeping preview headless while preserving a graphical Run command.” The takeaway is that JVM system properties must be passed independently to each spawned JVM.

### Suggested Narrative

Begin with the intermittent orphaned window, map the four-process lifecycle, show the overlapping-start race and best-effort stop limitation, introduce serialized ownership plus cooperative and forced shutdown, then demonstrate headless Preview versus graphical Run and report the test evidence and remaining platform limits.

### Claims Requiring Human Review

Any claim that the fix covers every supported operating system requires normal release testing on Windows, macOS, and Linux; local automated evidence alone will not establish that. Any security claim must distinguish the clean production audit from the unresolved development-only audit findings. Normal technical and editorial review remains required before publication.

## Context and Orientation

`vscode-extension/src/preview-commands.ts` owns the VS Code webview, file watcher, frame polling timer, and a `PreviewClient`. `vscode-extension/src/preview-client.ts` starts Gradle or Maven as a child process. The build-tool preview task writes `preview-session.json`, starts a detached Java coordinator, and records its process identifier in that descriptor. The coordinator is `tooling-java/tooling-cli/src/main/java/com/totalcross/tooling/cli/ToolingCli.java`; it starts a separate Java worker that loads the application preview runtime. Frames are written to `preview-frame.png`, and commands such as resize, reload, and stop are appended to `preview-control.txt`.

“Headless” means the Java Abstract Window Toolkit is forbidden from connecting to or creating a desktop display. It is enabled with the JVM system property `-Djava.awt.headless=true`. Because the coordinator starts the worker as another JVM, both command lines contain the property in updated Java tooling. The extension also sets `JAVA_TOOL_OPTIONS` for its preview launcher so older published tooling inherits the property throughout the process tree. The `run` presentation mode intentionally creates `AwtPreviewWindow` and therefore must not receive it from direct build-tool commands.

The worktree began with user-owned modifications in `tooling-java/tooling-core/src/main/java/com/totalcross/tooling/deploy/LegacyDeployService.java`, new tests under its deploy package, and generated Java build directories. This plan must not edit, remove, or clean those paths.

## Plan of Work

First, extend `vscode-extension/src/test/suite/preview-client.test.ts` to prove the generated Preview commands, serialized stop behavior, and manager replacement behavior. Refactor only enough process creation to make those behaviors deterministic under tests.

Then update `vscode-extension/src/preview-client.ts` so a stop waits for an in-flight build-tool preview start to settle before invoking the matching stop task, does not let an old child clear a newer process reference, and reports stop failure instead of silently swallowing it. Update `vscode-extension/src/preview-commands.ts` so `stop()` returns a promise, clears timers and watchers safely, and `start()` awaits cleanup of any owned prior session before assigning a new client.

Next, update the Gradle and Maven preview command builders so `preview` inserts `-Djava.awt.headless=true` before the classpath, while `run` does not. Update `ToolingCli` so its worker command receives the same preview-only property. Add focused command-construction tests to the existing Gradle, Maven, and tooling CLI suites.

Finally, update both plugin stop implementations. Append a stop command to the control file when possible, wait briefly for the coordinator to exit, then destroy descendants and the coordinator, wait again, and forcibly terminate survivors. Delete `preview-session.json` only after the termination routine has run. Add focused tests for command construction and any extracted termination behavior that can be exercised portably.

## Concrete Steps

From `vscode-extension`, run TypeScript compilation and the extension test suite:

    npm run compile
    npm test

From `tooling-java`, run the tooling CLI and host tests needed by the worker command and shutdown lifecycle:

    ./gradlew :tooling-cli:test :preview-host:test

Publish the modified local tooling modules to an isolated staging repository if needed, then run the Gradle plugin tests from `gradle-plugin` and Maven plugin tests from `maven-plugin` using the matching snapshot coordinates. Record the exact successful commands and staging location here rather than assuming a globally installed snapshot.

Run repository governance checks from `vscode-extension` after focused behavior tests:

    python3 tools/check-repository-governance.py
    python3 -m unittest tests.test_repository_governance

Expected focused evidence is that TypeScript compiles, lifecycle unit tests pass, preview command tests contain `-Djava.awt.headless=true`, run command tests do not, and process termination tests observe no live coordinator/worker after the bounded stop routine.

## Validation and Acceptance

Automated acceptance requires tests that fail on the pre-change code and pass after implementation for: repeated Preview cleaning the prior client; Stop Preview waiting for startup before the build-tool stop task; preview coordinator and worker commands containing the headless property; Run commands remaining graphical; and the stop routine escalating when cooperative shutdown does not finish.

Manual acceptance, when a compatible sample project and TotalCross SDK are available, is: invoke TotalCross: Preview, observe frames only in the VS Code webview with no separate Java window, invoke TotalCross: Stop Preview during both steady state and startup, and verify that the PID recorded in the preview session plus its descendants are no longer alive. Repeat Preview twice and verify that only the newest session remains. Invoke TotalCross: Run separately and verify that its intended desktop window still opens.

## Idempotence and Recovery

Compilation and test commands are safe to rerun. Tests must use temporary projects and terminate every process they create in `finally` blocks. Do not delete the pre-existing generated `tooling-java` directories because they belong to the user’s current work. If a test leaves a fixture process alive, identify its exact recorded PID, verify its command line, and terminate only that process tree.

## Artifacts and Notes

The initial worktree state is recorded in `Progress` and `Context and Orientation`. Keep verbose Java and VS Code integration output in temporary log files if necessary and add only concise pass/fail excerpts here.

## Interfaces and Dependencies

`PreviewClient.stop()` and `PreviewManager.stop()` will return `Promise<void>` so command handlers can await completed shutdown. Any process-spawn abstraction added for tests must retain Node’s `child_process.spawn` behavior in production and remain private or narrowly exported for tests.

The Gradle and Maven preview command builders retain their existing `previewCommand(Path toolingJdk, String mode, Path frame, Path control)` interface. Their returned list will add `-Djava.awt.headless=true` only when `mode` is `preview`. `ToolingCli` will expose a package-private worker-command builder to its tests if needed; production callers remain internal to the CLI.

Revision note (2026-08-25 04:51Z): Created the initial plan after tracing the complete VS Code, build-tool, coordinator, and worker lifecycle and identifying the overlapping ownership and unbounded stop risks.

Revision note (2026-08-25 05:04Z): Finalized the plan after implementation. Recorded the shared terminator design, headless coordinator and worker commands, unique-version staging workaround, complete automated validation, deferred manual SDK exercise, and npm development-audit limitation.

Revision note (2026-08-25 19:07Z): Reopened the completed plan after a real-project report showed that published beta Java artifacts did not contain the source-level headless changes. Added extension-owned environment propagation, focused tests, and macOS runtime validation against `tc-sample`, then updated the retrospective and Editorial Report.
