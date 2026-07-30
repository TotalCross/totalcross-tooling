<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Publish and verify the pre-IR release

This ExecPlan is Plan 08R. Start only after Plans 08C and 08D are accepted.
Publication is an explicit user-approved action.

## Purpose / Big Picture

Freeze, stage, publish, and verify a supported TotalCross release before the IR
merge. Produce a reproducible rollback point and compatibility matrix for branch
422 integration.

## Working Set and Resume Protocol

Read state, this plan, the Plan-08C and Plan-08D outcomes, and only publication
files for SDK artifacts, tooling modules, JDK catalog, plugins, companion, VS
Code extension, workflows, POMs, changelogs, and documentation.

Do not merge IR or move source ownership.

## Progress

- [x] (2026-07-30) Verify every Plan-08C and Plan-08D gate against exact feature commits.
- [x] (2026-07-30) Choose coordinated non-SNAPSHOT beta versions.
- [ ] Create release branches from accepted feature commits.
- [ ] Freeze scope and allow only release fixes.
- [x] (2026-07-30) Create one release manifest.
- [x] (2026-07-30) Record JDK catalog schema, entries, platforms, and resource checksum.
- [x] (2026-07-30) Parameterize Gradle and Maven release versions and staging repositories.
- [ ] Verify every catalog URL, SHA-256, and declared JAVA_HOME.
- [ ] Configure Java metadata, sources, Javadocs, signing, and staging.
- [ ] Configure Maven plugin Central publication and Invoker tests.
- [ ] Configure Gradle Plugin Portal metadata and validation.
- [ ] Configure companion archive checksums and release metadata.
- [ ] Bundle and inspect the VS Code extension.
- [ ] Replace extension defaults with released plugin coordinates.
- [ ] Publish the dependency chain to non-public staging.
- [ ] Consume staging from empty caches and a fresh store.
- [ ] Prove catalog-backed JDK installation and offline reuse.
- [ ] Repeat preview, package, reminder, and conversion E2E.
- [ ] Obtain explicit approval for public publication.
- [ ] Publish in dependency order.
- [ ] Verify public clean-room consumption.
- [ ] Tag exact commits and publish release notes.
- [ ] Update state to Plan 09.

## Current Architecture and Scope

Use a beta unless equivalent external testing justifies an RC. Do not reuse an
already published SDK version. No public artifact may reference a SNAPSHOT,
`mavenLocal`, local file path, unpublished dependency, floating JDK URL,
placeholder checksum, or development extension directory.

Release branches:

    totalcross:
      release/<sdk-version>

    totalcross-tooling:
      release/<tooling-release-version>

Feature branches remain available for Plan 09.

The candidate versions and exact feature-branch commits are recorded in
`.agent/reports/unified-tooling-preview-pre-ir-beta-1-manifest.json`. The
manifest is planning evidence only: no release branch, staging publication, or
public publication has been created.


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

Create a release manifest mapping:

    component, version, commit, license, and coordinate
    minimum Java, Maven, Gradle, and VS Code versions
    SDK compatibility range
    protocol, project-model, conversion-plan, and JDK-catalog schema versions
    bundled JDK catalog entry IDs and supported hosts
    catalog resource SHA-256
    artifact checksums and publication status

Provide complete POM metadata, sources, Javadocs, signatures, SCM, developers,
licenses, and reproducible checksums where applicable. Disclose the Java-17
plugin requirement and accepted aggregate compatibility waiver.

### JDK catalog release gate

For every shipped catalog entry:

    download from the committed concrete URL
    verify the committed SHA-256
    verify archive type and declared JAVA_HOME
    run capability probes on the matching available host
    record entry ID, vendor, version, build, and platform in the manifest

The release must document:

    automatic-installation host matrix
    explicit jdkPath override
    jdkPath fallback for unsupported hosts
    offline reuse behavior
    catalog update procedure
    policy rejecting floating URLs and unsupported CRaC builds

No production artifact may install from a dynamic provider response. Catalog
changes after public publication require a new tooling version unless a separate
signed and versioned update mechanism has been implemented and validated.

Run Maven Invoker tests on the supported matrix. Configure Gradle Plugin Portal
metadata and run `publishPlugins --validate-only`. Confirm one public package task
and document Gradle-task versus CLI bootstrap conversion.

Build a versioned companion archive or documented store install. Bundle
JavaScript and runtime dependencies. No manual `extraClasspath` or source checkout
is permitted.

The published extension default references the released Gradle plugin. Latest
SDK fallback remains dynamic through the shared SDK catalog, not hardcoded in
TypeScript.

Use empty temporary locations for:

    Gradle cache
    Maven repository
    TotalCross shared store
    VS Code profile
    project-conversion fixtures

Test catalog-backed online JDK installation, then remove network access and prove
offline reuse. Also test an unsupported-host fixture and its actionable
`jdkPath` message.

Repeat:

    process replacement and failed-candidate preservation
    Gradle and Maven preview, run, stop, and package
    wrapper preference
    installed VSIX activation
    reminder suppression and reset
    Maven conversion and rollback
    arbitrary-folder analysis, apply, validate, and rollback
    Gradle-task and CLI plan equivalence
    dynamic SDK and Java fallback
    pointer scaling, resize, orientation, and device profile

Present exact versions, checksums, commits, limitations, catalog coverage, and
skipped hosts. Do not publish publicly without explicit approval.

Publish in order:

1. SDK aggregate and narrow artifacts;
2. tooling core, protocol, conversion module, and bundled JDK catalog;
3. host, worker, CLI, and companion;
4. Maven plugin;
5. Gradle plugin;
6. VS Code extension.

Verify each component before publishing dependents. Repeat clean public
consumption, then create annotated tags and releases.

## Decision Log

- Decision: Plans 08C and 08D are hard prerequisites.
  Rationale: release packaging must not hide lifecycle, resolver, or migration
  defects.
  Date/Author: 2026-07-29 / User and OpenAI.

- Decision: the release manifest records the exact immutable JDK catalog.
  Rationale: automatic JDK installation is part of the released dependency chain
  and must be reproducible from reviewed metadata.
  Date/Author: 2026-07-29 / User and OpenAI.

- Decision: public publication requires explicit approval.
  Rationale: publication and tags are durable external actions.
  Date/Author: 2026-07-28 / OpenAI.

## Validation and Acceptance

Acceptance requires:

    no SNAPSHOT, mavenLocal, local path, or floating JDK URL
    full release manifest
    catalog schema and resource checksum recorded
    every catalog entry has a concrete URL and verified SHA-256
    minimum release host matrix is covered or explicitly reduced and approved
    staged artifacts resolve from empty caches
    automatic JDK installation and offline reuse pass
    unsupported-host jdkPath diagnostic passes
    Gradle Plugin Portal validation passes
    Maven Invoker matrix passes
    installed bundled VSIX passes all workflows
    failed build and candidate preserve active preview
    worker process replacement is observed
    reminder suppression is project-scoped and resettable
    Gradle task and CLI share the conversion engine and schema
    legacy conversion rolls back safely
    aggregate SDK smoke tests pass
    public checksums match the manifest
    exact release commits are tagged after approval

## Risks and Open Questions

A vendor may remove an archive after release. Retain release evidence and
checksums; consider mirroring only under a separately reviewed licensing and
distribution policy.

Marketplace, Plugin Portal, or Maven Central review may delay visibility. Record
pending status and do not claim publication until public resolution works.

Credentials stay outside committed files. Failed immutable publication uses a
new pre-release version.

## Idempotence and Recovery

Staging versions are replaced only where permitted. Public versions are
immutable. Release branches accept only focused fixes. A catalog correction after
public release requires a new tooling version.

## Outcomes & Retrospective

Not started.

## Revision Note

2026-07-29: added immutable JDK catalog manifest, host-matrix, clean-install,
offline-reuse, and publication gates.

2026-07-29: added Plans 08C and 08D as release prerequisites.

2026-07-30: verified the accepted 08C/08D feature heads, selected beta
coordinates, and recorded the immutable catalog resource checksum and host
entries in the pre-IR planning manifest.

2026-07-30: Gradle release mode now resolves the beta tooling chain from a
staging repository without `mavenLocal`; Maven uses the same configurable
tooling version and an opt-in staging profile.


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
