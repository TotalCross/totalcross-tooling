<!--
Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->

# Current main history audit

This report records a read-only comparison performed on 2026-07-16 against
`main` at `4645479`. The source mirrors were read from the immutable workspace
under `../tooling-migration/source-audit/`; no source mirror or Git history was
modified.

The comparison located source commits in the current monorepo by matching the
original author name, author email, author timestamp, complete commit message,
and the source changed paths after prefixing them with the final project
directory. It then compared committer metadata and mapped parent relationships.
This is an audit of the history that is actually in `main`; it is not an
assertion that a filter-repo commit-map artifact exists.

| Project | Source revision | Source commits | Located in `main` | Committer mismatches | Parent mismatches |
| --- | --- | ---: | ---: | ---: | ---: |
| Maven | `cdaa64dd789bca59cf3e32013ac32c6ec29e8bf2` | 83 | 83 | 0 | 0 |
| VS Code | `23e4c2ccaf3a205d1f02d3aa8c7a834609d396aa` | 68 | 68 | 68 | 2 |
| Gradle | `c6aa342f0f44f46bbcf9eac369372a5982786654` | 6 | 6 | 6 | 1 |

All source commit messages and author metadata matched the located commits.
The mismatches are material to the original history-preservation requirement:
the VS Code and Gradle commits use migration-period committer metadata, and
the project-root boundaries add parent relationships that did not exist in the
individual source histories. Rewriting `main` to repair those objects would
require a history rewrite and force-push, which repository instructions forbid.

The supported cross-repository tool remains
`tools/check-imported-history.py`. It audits a filtered history only when an
external source root, a source-revision manifest, and old-to-new mapping inputs
are supplied explicitly. Those inputs are intentionally not fabricated from
this failed exact-preservation comparison.
