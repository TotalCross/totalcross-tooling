<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Correct the pre-IR preview and plugin architecture

This ExecPlan is Plan 08C. It is a release-blocking correction after the Plan
08B audit. Start from the accepted feature commits; do not create release
branches until this plan passes.

## Purpose / Big Picture

Make implemented behavior match the declared architecture: a persistent
coordinator promotes disposable worker processes only after a valid first frame,
and failed builds or candidates preserve the previous application.

Make Gradle, Maven, CLI, and VS Code consume the same project model, store, JDK
selection, compatibility policy, and public tasks. Automatic tooling-JDK
installation must consume a reviewed immutable catalog; explicit `jdkPath`
remains supported and is required only when the host is not covered.

## Working Set and Resume Protocol

Read state and this plan. Inspect only:

    tooling-java host, worker, coordinator, CLI, codecs, and resolvers
    tooling-core JDK providers, selector, store, download, and process packages
    Gradle preview, run, stop, model, package, extension, and build metadata
    Maven preview, run, stop, package, JDK/SDK managers, POM, and tests
    VS Code preview client, Webview, project layout, settings, and packaging
    TotalCross preview contract documentation and focused tests
    current state and evidence

Do not implement the new project converter in this plan. Do not merge IR, move
converter source, create tags, or publish publicly.

## Progress

### Reconciliation

- [x] Record current remote heads and remove stale local-only SHAs from state.
- [x] Replace absolute machine paths with repository-relative resume commands.
- [x] Record the post-08B audit findings in evidence.

### Process-isolated preview

- [x] Add a process-backed candidate implementation.
- [x] Connect `PreviewReloadCoordinator` to production CLI execution.
- [x] Keep one coordinator alive while worker candidates are replaced.
- [x] Require authentication, ready, and first frame before promotion.
- [x] Preserve the active worker after timeout, error, or invalid frame.
- [x] Route input and stop only to the promoted worker.
- [x] Close the previous worker and descendants only after promotion.
- [x] Prove repeated reload leaves no old worker processes.
- [x] Define distinct preview and run presentation modes.

### Shared resolution and immutable JDK catalog

- [x] Run capability probes before using a tooling JDK.
- [x] Define and parse a versioned immutable JDK catalog.
- [x] Add bundled, file, and test catalog sources behind one interface.
- [x] Add concrete JDK versions, URLs, SHA-256 values, and archive layouts.
- [x] Cover the minimum pre-IR release host matrix.
- [x] Install catalog JDKs through the immutable shared store.
- [x] Make `jdkPath` the probed highest-priority override.
- [x] Add actionable `jdkPath` fallback for unsupported hosts.
- [x] Restrict dynamic providers to catalog-maintenance workflows.
- [x] Make shared SDK/JDK/store services authoritative in Gradle, Maven, and CLI.
- [x] Remove the Maven Zulu-only, latest, x86 JDK download path.
- [x] Enforce and document Java 17 for loading both pre-IR plugins.
- [x] Serialize and parse the complete versioned ProjectModel.
- [x] Include real roots, outputs, dependencies, SDK, targets, and arguments.
- [x] Remove or internalize `totalcrossTypedPackage`.
- [ ] Remove obsolete resolver/download dependencies after equivalence tests.
- [ ] Clarify internal versus external preview contract documentation.

### VS Code corrections

- [ ] Build successfully before requesting a candidate reload.
- [ ] Keep the displayed frame and session after a failed build.
- [ ] Prefer Maven Wrapper when present.
- [ ] Scale pointer coordinates to intrinsic frame pixels.
- [ ] Send resize and apply device size, density, and orientation.
- [ ] Bundle production dependencies deterministically into the VSIX.
- [ ] Remove SNAPSHOT and `publishToMavenLocal` from release defaults.
- [ ] Pass focused and installed-VSIX E2E tests.
- [ ] Commit focused slices and update state to Plan 08D.


## Cross-plan safety and size policy

Run one plan at a time and preserve unrelated work. Never use `git reset --hard`,
`git clean -fd`, force-push, history rewriting, tag deletion, or repository
archival unless the user explicitly requests that exact operation.

Every implementation text file created or modified in the TotalCross repositories
must remain at or below 20 KiB and approximately 600 lines. Run the staged
size-policy checker before every commit. Split an oversized non-protected file by
responsibility before changing its behavior. Do not split a protected IR file
merely to satisfy this rule.

Protected paths:

    TotalCrossSDK/src/main/java/tc/tools/converter/**
    TotalCrossSDK/src/test/java/tc/tools/converter/**
    TotalCrossVM/src/tcvm/ir/**
    TotalCrossVM/src/tcvm/jit/**
    TotalCrossVM/src/tcvm/aot/**
    TotalCrossVM/src/tests/ir/**
    docs/architecture/bytecode/**

A project-conversion operation may relocate a pre-existing user file without
rewriting or splitting its contents, even when that file is larger. Newly
generated build files, reports, journals, and implementation files remain
subject to the limit. Split a large migration journal into numbered chunks.

Generated build output, caches, downloaded tools, credentials, and third-party
content must not be committed.

Use token-efficient execution. Read the state file and active plan first. Inspect
only named paths. Save verbose logs under `/tmp` or build artifacts and record
only concise outcomes, commit IDs, and log paths. Do not repeatedly print full
plans, diffs, generated projects, dependency trees, catalogs, or test logs.

## Plan of Work

Execute one milestone at a time. Commit each accepted milestone before reading
implementation paths for the next.

### Milestone 1: reconcile state and evidence

This milestone is complete. Preserve Plan 08B evidence and the recorded audit
findings. Do not rewrite historical outcomes.

### Milestone 2: process-backed candidate promotion

The process-backed promotion slice is complete. Preserve its tests and evidence.
Finish only the remaining presentation-mode distinction:

    preview:
      frame stream, frame file, or IDE adapter

    run:
      native AWT window owned by the coordinator

Both modes use the same worker lifecycle and promotion rules.

### Milestone 3: immutable JDK catalog and authoritative resolution

Create one versioned catalog model. A catalog entry contains at least:

    schemaVersion
    entryId
    vendor
    javaMajor
    version
    build
    operatingSystem
    architecture
    archiveType
    url
    sha256
    javaHomeRelativePath
    archiveSize when known
    releaseDate when known

The representation may be JSON or another small versioned format. The parser
must not be coupled to a classpath resource. Define a source interface, for
example:

    JdkCatalogSource
      BundledJdkCatalogSource
      FileJdkCatalogSource
      CompositeJdkCatalogSource

Tests use an in-memory or fixture source. Future remotely updated metadata is out
of scope unless it is separately signed, versioned, cached, and verified.

Catalog entries use concrete versions and final archive URLs. Reject:

    latest or floating release URLs
    missing or placeholder SHA-256 values
    unsupported CRaC or specialized builds
    unknown operating systems or architectures
    archive layouts without a declared JAVA_HOME path

Selection precedence:

1. explicit `jdkPath`;
2. explicitly permitted compatible `JAVA_HOME`;
3. an existing verified shared-store installation;
4. bundled immutable catalog candidates;
5. future verified catalog updates, when implemented;
6. a clear failure requesting `jdkPath`.

Every selected candidate, including local paths, must pass:

    java -version
    javac -version
    ProcessBuilder child with stdout and stderr
    timeout and process-tree cleanup
    xattr execution on macOS
    protoc --version when protoc is available

An absent quarantine attribute is not a failure. Inability to create or wait for
the process is a failure.

Install catalog candidates through the existing store:

    download to unique staging
    verify SHA-256 before extraction
    reject archive traversal
    extract and locate declared JAVA_HOME
    run capability probes
    write immutable metadata and completion marker
    atomically promote to the final concrete-version directory

Installation metadata records catalog schema, entry ID, vendor, version, host,
URL, SHA-256, and selected JAVA_HOME.

Minimum release matrix:

    Java 17 / macOS ARM64
    Java 17 / macOS x64
    Java 17 / Linux x64
    Java 17 / Windows x64

Add Linux ARM64 when a verified supported archive is available. One primary and
one fallback vendor may be cataloged, but vendor name never bypasses probes.

Dynamic `CorrettoProvider`, `TemurinProvider`, and `ZuluProvider` implementations
may remain only as catalog-maintenance utilities:

    discover concrete release
    download candidate bytes
    calculate SHA-256
    emit a candidate catalog entry
    require human or CI review before commit

Production resolution must not install directly from provider responses.

Create or complete `ToolingEnvironmentResolver` so Gradle, Maven, CLI, and the VS
Code companion use this policy. Remove Maven `JavaJDKManager` and Gradle-local
JDK download paths only after focused equivalence tests. Remove unused AWS,
appdirs, zip, and downloader dependencies.

Both plugins load on Java 17 for this release. Add startup diagnostics, README
matrices, Gradle tests, Maven prerequisite metadata where supported, and failure
tests for older JVMs. Application bytecode target remains independent.

Catalog tests must cover:

    schema version and malformed entries
    unsupported host and architecture
    concrete candidate selection
    explicit jdkPath precedence and invalid path
    capability-probe failure
    checksum mismatch and archive traversal
    interrupted and concurrent installation
    incorrect javaHomeRelativePath
    offline reuse
    candidate fallback
    missing entry with actionable jdkPath message
    floating URL or placeholder hash rejection
    unsupported CRaC rejection

### Milestone 4: complete project model and tasks

Add a versioned `ProjectModelCodec`. Serialize the entire model:

    schema version
    build tool and project root
    main and test source roots
    resource roots and outputs
    dependency classpath
    MainWindow class
    SDK version and coordinate
    tooling JDK catalog entry or explicit-path identity
    application Java target
    Retrolambda plan
    Launcher and Deploy arguments
    platforms and preview descriptor

Do not store credentials or unnecessary cache paths.

Gradle and Maven must produce semantically equivalent models. CLI and VS Code
consume the model rather than rediscovering values differently.

Keep only `totalcrossPackage` and `totalcross:package` as public package entry
points. Remove or internalize `totalcrossTypedPackage`. Document preview, run,
and stop semantics consistently.

Document SDK in-process preview types as internal compatibility surfaces. The
cross-process frame and command protocol is the stable external boundary.

### Milestone 5: correct VS Code preview

Reload order:

    build
      failure -> report error and retain current session and frame
      success -> request a candidate reload
    await promoted event
    update diagnostics and frame

Prefer wrappers:

    Gradle: gradlew or gradlew.bat
    Maven: mvnw or mvnw.cmd
    system executable only when no wrapper exists

Track intrinsic and displayed frame dimensions. Scale pointer coordinates before
sending. Use `ResizeObserver` and device-profile settings to send width, height,
density, and orientation.

The companion obtains its JDK through the shared resolver or an explicit
configured `jdkPath`; it must not assume `java` on PATH is compatible.

Bundle the extension with a supported bundler or deterministic dependency
package. Inspect and install the VSIX without source checkout or development
`node_modules`.

### Milestone 6: validation

Acceptance requires:

1. twenty process-replacement reloads leave only the active worker;
2. candidate failure preserves prior frame and process;
3. VS Code build failure preserves active preview;
4. pointer scaling and resize reach the worker correctly;
5. Gradle, Maven, CLI, and VS Code consume equivalent project models;
6. no public plugin path uses legacy JDK/SDK downloaders;
7. only one public package task exists per build tool;
8. installed bundled VSIX passes preview, reload, failed build, recovery, stop;
9. no SNAPSHOT or `mavenLocal` remains in release-facing defaults;
10. every downloaded JDK uses a committed concrete URL and SHA-256;
11. clean-cache installation and offline reuse pass on the release matrix;
12. an unsupported host produces an actionable `jdkPath` diagnostic;
13. no production path uses `latest`, forces `arch=x86`, or accepts CRaC
    implicitly.

Run focused tests per milestone, then the justified full matrix. Run
`git diff --check`, license checks, dependency analysis, and staged size checks.

## Decision Log

- Decision: worker replacement is a release gate.
  Rationale: MainWindow replacement does not guarantee application state
  isolation.
  Date/Author: 2026-07-29 / OpenAI.

- Decision: introduce a versioned immutable JDK catalog and retain `jdkPath` as
  an explicit override and unsupported-platform fallback.
  Rationale: automatic installation must be reproducible and checksum-verified.
  A globally mandatory `jdkPath` would regress tooling usability, while dynamic
  provider URLs cannot define immutable installations safely.
  Date/Author: 2026-07-29 / User and OpenAI.

- Decision: dynamic vendor providers are catalog-maintenance tools only.
  Rationale: runtime selection must consume reviewed bytes and committed hashes,
  not mutable provider responses.
  Date/Author: 2026-07-29 / OpenAI.

- Decision: plugin loading requires Java 17 for the pre-IR release.
  Rationale: current plugin and shared tooling bytecode already require it.
  Date/Author: 2026-07-29 / OpenAI.

- Decision: project conversion is deferred to Plan 08D.
  Rationale: architecture correction and migration analysis are separate bounded
  contexts.
  Date/Author: 2026-07-29 / OpenAI.

## Validation and Acceptance

Plan 08C completes only when every open progress item and Milestone-6 acceptance
item passes. Coordinator unit tests or catalog parser tests alone are
insufficient; real CLI, plugins, installed extension, store installation, and
offline reuse must use the completed policy.

Record unavailable platform tests honestly. A host omitted from the catalog is
unsupported for automatic installation, not silently accepted through a floating
URL.

## Risks and Open Questions

Vendor archives and URLs may be removed. Catalog maintenance must verify the
actual downloaded bytes before updating a committed entry. A future remotely
updated catalog requires a separate signed-update design.

Changing process and JDK ownership may expose assumptions in detached Maven or
Gradle sessions. Prefer a shared session record and authenticated control channel
over PID parsing.

## Idempotence and Recovery

Each milestone is separately commit-ready and revertible. Candidate failure does
not change active preview ownership. JDK installation never replaces a completed
version until staging, checksum, extraction, and probes pass. Plan 08R remains
blocked.

## Outcomes & Retrospective

In progress. Reconciliation, process-backed promotion, failed-candidate
preservation, repeated real-worker replacement, and initial capability-probe
integration are complete. Immutable catalog materialization and remaining
plugin/editor corrections are pending.

## Revision Note

2026-07-29: added immutable JDK catalog schema, runtime/maintenance separation,
minimum host matrix, tests, and `jdkPath` fallback decision.

2026-07-29: created from the post-Plan-08B audit.


## Editorial Report

Complete this section only from executed evidence.

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
