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
- [x] (2026-07-16) Pushed annotated tag `v0.0.16` at `e6fdae9` and the
  bootstrap workflow commits `f963600` and `572ab8b` to `main`.
- [x] (2026-07-16) Observed successful Actions run `29540852105`, GitHub
  release `v0.0.16`, its `.vsix` asset, and the Marketplace package endpoint
  for version 0.0.16.
- [x] (2026-07-16) Finalized the Editorial Report from observed evidence.

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
- Observation: the first remote bootstrap run stopped at release-version
  validation because the Node expression contained literal escaped quotes.
  Evidence: Actions run `29540788570` failed in `Validate release version`
  with exit code 2 before the package or publish steps.
- Observation: the Marketplace catalog query continued to return 0.0.15 after
  the successful publish, while the version-specific package endpoint returned
  HTTP 200 for 0.0.16.
  Evidence: `POST extensionquery` listed cached 0.0.15; `GET
  .../vscode-totalcross/0.0.16/vspackage` returned HTTP 200.

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

Version 0.0.16 was published from `e6fdae9`. The repeatable workflow now lives
in `.github/workflows/publish-vscode-extension.yml`; `v0.0.16` is an annotated
remote tag, Actions run `29540852105` completed successfully, and the release
contains `vscode-totalcross-0.0.16.vsix`. The extension-local legacy governance
validator remains an intentionally deferred mismatch.

## Editorial Report

### Editorial Summary

The repository can publish the TotalCross VS Code extension without exposing a
Marketplace token in Git. Version 0.0.16 was packaged from `e6fdae9`, published
through GitHub Actions, tagged, and released with its `.vsix` artifact.

### Original Plan versus Actual Outcome

The original goal was delivered. The workflow was added after the historical
release commit, so a one-time guarded bootstrap push was used instead of
rewriting the tag. The legacy extension-local governance validator was left
unchanged because its repair would be an unrelated bulk header migration.

### What Changed

`.github/workflows/publish-vscode-extension.yml` validates the tag and package
version, packages a VSIX, publishes with `VSCE_PAT`, and creates or updates a
GitHub release. `.agent/exec-plan-vscode-0.0.16-publication.md` records the
historical bootstrap and its evidence.

### Decisions and Trade-offs

The bootstrap decision is recorded above. It is limited to the historical
0.0.16 release because the workflow cannot be retroactively added to its tag.

### Unexpected Problems and Discoveries

The missing workflow at the historical tagged commit required a branch-push
bootstrap. The first bootstrap run failed because an escaped quote reached the
Node command literally; commit `572ab8b` corrected it and the second run
succeeded.

### Validation and Measurable Results

Observed locally: `npm ci` and `npm run compile` completed successfully in
`vscode-extension`; `python3 tools/check-license-headers.py --root .` passed.
Actions run `29540852105` completed successfully. GitHub release `v0.0.16`
targets `e6fdae94e814437d0f7cf0746d8042dcce1f1164` and serves
`vscode-totalcross-0.0.16.vsix`. The Marketplace version-specific package
endpoint returned HTTP 200 for 0.0.16. The extension-local governance command
still fails for the documented historical-header mismatch.

### Useful Evidence and Examples

Useful evidence includes Actions run `29540852105`, release
`https://github.com/TotalCross/totalcross-tooling/releases/tag/v0.0.16`, and
the release asset `vscode-totalcross-0.0.16.vsix`.

### Limitations, Remaining Work, and Open Questions

Future publication remains dependent on `VSCE_PAT` retaining permission under
the `TotalCross` publisher. Marketplace catalog listings may lag the successful
version-specific package endpoint. The legacy extension-local header validator
still needs a separately scoped reconciliation with the monorepo policy.

### Possible Article Angles

A release-engineering article could show how to bootstrap a historical tagged
release without changing the release commit or exposing a Marketplace token.

### Suggested Narrative

Describe the historical-release constraint, the tag and workflow split, the
secret-backed Marketplace publish step, and the GitHub release artifact.

### Claims Requiring Human Review

No special claims remain beyond normal verification that the Marketplace UI has
finished propagating its cached catalog listing.

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
Updated after the first remote bootstrap attempt to record and correct the
escaped-quote failure in the version check.
Updated after the successful remote run to record release, asset, and
Marketplace endpoint evidence.
