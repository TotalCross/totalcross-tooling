<!--
Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->

# Publish VS Code extension 0.0.16

This ExecPlan is maintained under `.agent/PLANS.md`. It records the one-time
publication of the TotalCross VS Code extension at version 0.0.16 and leaves a
repeatable GitHub Actions workflow for later versions.

## Purpose / Big Picture

After this work, the Visual Studio Marketplace and the GitHub releases page
will expose version 0.0.16 built from commit `e6fdae9`, the commit that updates
the extension version and monorepo URL. A maintainer can later publish another
tag through the same workflow without placing a marketplace token in the
repository.

## Progress

- [x] (2026-07-16) Confirmed `main`, commit `e6fdae9`, local tag `v0.0.16`,
  package version, and the repository secret name `VSCE_PAT` supplied by the
  maintainer.
- [x] (2026-07-16) Added the publication workflow and confirmed its release
  version check and TypeScript compilation.
- [ ] Push `v0.0.16` at `e6fdae9`, push the workflow bootstrap commit, and
  observe marketplace publication and the GitHub release.
- [ ] Record the actual remote publication evidence and finalize this plan.

## Surprises & Discoveries

- Observation: a tag push cannot run a workflow that did not exist at the
  tagged commit. The release commit predates this workflow.
  Evidence: `e6fdae9` is the requested release commit and its tree has no
  `.github/workflows/publish-vscode-extension.yml`.
- Observation: `python3 tools/check-repository-governance.py` inside the
  extension still expects the former per-extension MIT header policy and fails
  after the monorepo-wide Apache header normalization.
  Evidence: the command reports historical MIT headers missing in imported
  TypeScript files, while the root `tools/check-license-headers.py` policy is
  the policy normalized by the final migration commit.

## Decision Log

- Decision: use a narrowly guarded push-to-`main` bootstrap only for version
  0.0.16, then use tag pushes or `workflow_dispatch` for every later release.
  Rationale: it permits a release from the historical release commit while
  keeping the tagged commit unchanged and avoids storing `VSCE_PAT` locally.
  Date/Author: 2026-07-16 / Codex.
- Decision: do not repair the extension's legacy header validator as part of
  the release workflow change.
  Rationale: the discrepancy predates this workflow and repairing it would
  change many imported source files outside the requested publication scope.
  Date/Author: 2026-07-16 / Codex.

## Outcomes & Retrospective

The workflow is implemented and TypeScript compilation succeeded. The tag,
Marketplace publication, and GitHub release remain pending. This section and
the Editorial Report will be updated with the actual Actions run and release
URLs once the remote run completes.

## Editorial Report

### Editorial Summary

The workflow is ready; remote publication is pending.

### Original Plan versus Actual Outcome

The original goal remains unchanged. The legacy extension-local governance
validator is not part of this release because it conflicts with the later
monorepo header policy.

### What Changed

`.github/workflows/publish-vscode-extension.yml` packages, publishes, and
releases the extension. This ExecPlan records the bootstrap and its evidence.

### Decisions and Trade-offs

The bootstrap decision is recorded above. It is limited to the historical
0.0.16 release because the workflow cannot be retroactively added to its tag.

### Unexpected Problems and Discoveries

The missing historical workflow is documented above.

### Validation and Measurable Results

Observed locally: `npm ci` and `npm run compile` completed successfully in
`vscode-extension`. The extension-local governance command failed for the
preexisting header-policy mismatch documented above.

### Useful Evidence and Examples

The final evidence will name the Actions run, `v0.0.16`, and its GitHub
release.

### Limitations, Remaining Work, and Open Questions

Publication remains dependent on the configured `VSCE_PAT` retaining permission
to publish under the `TotalCross` marketplace publisher. The final remote run
and public release evidence are still pending.

### Possible Article Angles

A release-engineering article could show how to bootstrap a historical tagged
release without changing the release commit or exposing a Marketplace token.

### Suggested Narrative

Describe the historical-release constraint, the tag and workflow split, the
secret-backed Marketplace publish step, and the GitHub release artifact.

### Claims Requiring Human Review

The Marketplace publication and public release must be checked after the
remote workflow completes.

## Context and Orientation

`vscode-extension/package.json` defines the Marketplace publisher and version.
The `VSCE_PAT` GitHub Actions secret is a Visual Studio Marketplace personal
access token and is passed only as an environment variable to `@vscode/vsce`.
The workflow packages the extension from an exact Git commit, publishes that
package, and attaches the same `.vsix` file to a GitHub release.

## Plan of Work

Create `.github/workflows/publish-vscode-extension.yml`. It will validate that
the requested tag matches `package.json`, run `npm ci`, package a `.vsix`,
publish it with `VSCE_PAT`, and create or update the GitHub release. It accepts
manual `tag` and `ref` inputs, responds to future `v*` tags, and recognizes one
explicit bootstrap commit for the 0.0.16 historical release.

## Concrete Steps

From the repository root, run `npm ci` and `npm run compile` in
`vscode-extension`. Commit the workflow with the bootstrap subject, push the
already annotated `v0.0.16` tag to `e6fdae9`, then push `main`. The bootstrap
run must report a successful VSCE publish and create a release with the `.vsix`
asset. Later maintainers either push a `v*` tag whose name matches the package
version or dispatch the workflow with an explicit tag and commit.

## Validation and Acceptance

`npm run compile` must succeed. The workflow validates `v<package version>`
before publishing. GitHub Actions must complete successfully; the Marketplace
must show version 0.0.16 and GitHub must show release `v0.0.16` targeting
`e6fdae9` with the packaged `.vsix` asset.

## Idempotence and Recovery

The workflow uses `gh release upload --clobber` when a release already exists,
so reruns replace the release asset safely. Marketplace publication cannot
publish the same version twice; a rerun after a successful VSCE publish should
be treated as a release-asset retry rather than a second marketplace publish.

## Artifacts and Notes

The release asset is named `vscode-totalcross-<version>.vsix`. The tag remains
anchored at `e6fdae9`; the workflow commit is intentionally later on `main`.

## Interfaces and Dependencies

The workflow uses `actions/checkout@v4`, `actions/setup-node@v4`, Node 22,
`@vscode/vsce`, the `VSCE_PAT` repository secret, and the GitHub CLI available
on GitHub-hosted Ubuntu runners.

Revision note (2026-07-16): created to document the historical 0.0.16
publication workflow and its tag-preserving bootstrap. Updated after local
workflow validation to record the legacy header-validator mismatch.
