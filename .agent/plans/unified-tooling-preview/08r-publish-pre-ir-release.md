<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Publish and verify the pre-IR release

This ExecPlan is Plan 08R. Start only after Plan 08B acceptance. Publication is
an explicit user-approved action.

## Purpose / Big Picture

Freeze, stage, publish, and verify a supported TotalCross release before the IR
merge. Produce a reproducible rollback point and a compatibility matrix for the
subsequent branch-422 integration.

## Working Set and Resume Protocol

Read state, this plan, release metadata, and only the publication files for:

    TotalCross SDK and narrow Java artifacts
    tooling Java modules and companion distribution
    Maven plugin
    Gradle plugin
    VS Code extension
    release workflows, POMs, changelogs, and documentation

Do not merge IR or perform source ownership movement.

## Progress

- [ ] Choose coordinated non-SNAPSHOT beta versions.
- [ ] Create release branches from accepted feature commits.
- [ ] Freeze scope and allow only release fixes.
- [ ] Configure Java artifact metadata, sources, Javadocs, signing, and staging.
- [ ] Configure Maven plugin Central publication and Invoker tests.
- [ ] Configure Gradle Plugin Portal metadata and validation.
- [ ] Configure companion archive checksums and release.
- [ ] Consolidate and bundle the VS Code extension.
- [ ] Publish all components to a non-public staging repository.
- [ ] Consume staging from empty caches on supported hosts.
- [ ] Obtain explicit approval for public publication.
- [ ] Publish in dependency order.
- [ ] Verify public clean-room consumption.
- [ ] Tag exact commits and publish release notes.
- [ ] Update state to Plan 09.

## Current Architecture and Scope

Use a beta unless equivalent external pre-release testing already justifies an
RC. Example coordinated versions:

    SDK: <next-sdk-version>-beta.1
    tooling modules: 0.1.0-beta.1
    Gradle plugin: <next-gradle-plugin-version>-beta.1
    Maven plugin: <next-maven-plugin-version>-beta.1
    VS Code extension: <next-extension-version>

Do not reuse an already published SDK version. Do not publish any public artifact
that references `-SNAPSHOT`, `mavenLocal`, a local file path, or an unpublished
dependency.

Release branches:

    totalcross:
      release/<sdk-version>

    totalcross-tooling:
      release/<tooling-release-version>

The feature branches remain unchanged for Plan 09 after release completion.


## Cross-plan safety and size policy

Run one plan at a time. Preserve unrelated work. Never use `git reset --hard`,
`git clean -fd`, force-push, history rewriting, tag deletion, or repository
archival unless the user explicitly requests that exact operation.

Every created or modified text file must remain at or below 20 KiB and at or
below approximately 600 lines. Run the staged size-policy checker before every
commit. If an existing non-protected file exceeds either limit, split it by
responsibility before the functional change. Do not split a protected IR-related
file merely to satisfy this rule.

Protected paths:

    TotalCrossSDK/src/main/java/tc/tools/converter/**
    TotalCrossSDK/src/test/java/tc/tools/converter/**
    TotalCrossVM/src/tcvm/ir/**
    TotalCrossVM/src/tcvm/jit/**
    TotalCrossVM/src/tcvm/aot/**
    TotalCrossVM/src/tests/ir/**
    docs/architecture/bytecode/**

Generated files, third-party code, caches, and build output must not be committed.

Use token-efficient execution. Read the state file first, then the active plan.
Inspect only named paths. Store verbose output in `/tmp` or build artifacts and
record only concise results, commit IDs, and log paths. Do not repeatedly print
large plans, logs, generated files, or full repository diffs.

## Plan of Work

### Version and metadata

Create one release manifest mapping every component version, Git commit, license,
minimum Java/Maven/Gradle/VS Code version, SDK compatibility range, and public
coordinate. Keep module licenses accurate; do not relicense LGPL-derived code as
Apache-2.0.

For Maven-published artifacts provide complete POM metadata, sources JAR,
Javadoc JAR, signatures, SCM, developers, licenses, and reproducible checksums.
Publish aggregate and narrow SDK artifacts plus required tooling modules in
dependency order.

### Maven plugin

Resolve its final minimum JVM from Plan 08B. Run Maven Invoker tests against the
staging repository. Generate help documentation and list goals, parameters,
defaults, compatibility, and preview lifecycle.

### Gradle plugin

Apply and configure the Gradle Plugin Publish plugin. Add website, VCS URL, tags,
description, implementation artifact, compatibility declaration, and a
non-SNAPSHOT version. Run:

    publishPlugins --validate-only

Then test a clean consumer using the same coordinates planned for publication.

### Companion and VS Code

Build a versioned companion archive or define the checksummed store installation
metadata. The VSIX must not require manual `extraClasspath`.

Bundle TypeScript/JavaScript, exclude development dependencies and build
intermediates, update CHANGELOG and release notes, inspect VSIX contents, and
install the generated VSIX on test hosts. Keep wizard and conversion commands.

### Staging and clean-room validation

Publish all Java artifacts and plugins to a private or local HTTP staging
repository. Use temporary empty directories for:

    GRADLE_USER_HOME
    Maven local repository
    TotalCross shared store
    VS Code extension profile

Run the Plan-08B end-to-end matrix using only staged coordinates. Test offline
reuse only after one successful online resolution.

After successful staging, present exact versions, checksums, commits, known
limitations, and skipped hosts to the user. Do not perform public publication
until explicit approval is received.

### Public publication

Publish in this order:

1. SDK aggregate and narrow artifacts;
2. tooling-core and protocol;
3. preview host, worker, CLI, and companion;
4. Maven plugin;
5. Gradle plugin;
6. VS Code extension.

Verify each public artifact before publishing its dependents. Then repeat clean
consumption without staging or local repositories. Create annotated tags and
GitHub releases only after public verification.

## Decision Log

- Decision: release branches are cut after stabilization, not before.
  Rationale: feature work remains flexible until the end-to-end contract passes.
  Date/Author: 2026-07-28 / OpenAI.

- Decision: stage the complete dependency chain before public publication.
  Rationale: plugin and extension failures often appear only outside local caches.
  Date/Author: 2026-07-28 / OpenAI.

- Decision: public publication requires explicit approval.
  Rationale: artifact publication and tags are durable external actions.
  Date/Author: 2026-07-28 / OpenAI.

## Validation and Acceptance

Acceptance requires:

    no SNAPSHOT or mavenLocal references
    full release manifest
    all staged artifacts resolve from empty caches
    Gradle plugin validation passes
    Maven Invoker tests pass
    installed VSIX passes wizard, conversion, preview, reload, stop
    aggregate SDK compatibility smoke tests pass
    published checksums match downloaded bytes
    public consumers resolve after approval
    exact release commits are tagged
    release notes document preview minimum SDK and known limitations

Record every published coordinate, URL through repository metadata rather than raw
secrets, checksum, tag, and verification log.

## Risks and Open Questions

Marketplace, Plugin Portal, or Maven Central review may delay public visibility.
Record pending status and do not claim publication until public resolution works.

Credential configuration must stay outside committed files. A failed dependent
publication must not cause version reuse; increment the affected pre-release.

## Idempotence and Recovery

Staging versions may be replaced only when the staging repository permits it.
Public versions are immutable. Failed public publication uses a new version.
Release branches accept only focused release fixes.

## Outcomes & Retrospective

Not started.

## Revision Note

2026-07-28: added a separate user-approved publication plan with staging,
clean-cache consumption, dependency ordering, and release branches.

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
