<!--
Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->

# TotalCross Tooling

This repository consolidates the developer tooling for TotalCross while keeping
each project independently buildable, versioned, and released. The imported
projects are ordered in first-parent history as follows:

1. `maven-plugin/`
2. `vscode-extension/`
3. `gradle-plugin/`

Fabio Sobral ([@flsobral](https://github.com/flsobral)) is the sole current
maintainer. Releases remain independent and imported tags are namespaced as
`maven-plugin-*`, `vscode-extension-*`, and `gradle-plugin-*`.

## Build and validation

Run root provenance validation:

    python3 tools/check-license-headers.py
    python3 -m unittest discover -s tests/license_headers -p 'test_*.py'

Run a project from its own directory:

    cd maven-plugin && mvn --batch-mode --no-transfer-progress test
    cd vscode-extension && npm ci && npm run audit && npm run compile && npm test
    cd gradle-plugin && ./gradlew clean test --console=plain

`migration/license-provenance.json` and `migration/commit-maps/` record file
provenance and old-to-new commit translations. Git history imports source code
and commit identity, but does not transfer original issues, pull requests,
releases, stars, forks, or discussions. The original repositories remain the
historical location for those resources until their separate move notices and
archival decisions are completed.

See `.agent/exec-plan-consolidate-tooling-repositories.md` for the migration
record and `CONTRIBUTING.md` for local validation guidance.
