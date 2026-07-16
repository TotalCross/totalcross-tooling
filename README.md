<!--
Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->

# TotalCross Tooling

This repository consolidates the developer tooling for TotalCross while keeping
each project independently buildable, versioned, and released. It is being
populated in this order:

1. `maven-plugin/`
2. `vscode-extension/`
3. `gradle-plugin/`

Until the migration is complete, these directories are planned locations rather
than imported implementations. Fabio Sobral ([@flsobral](https://github.com/flsobral))
is the sole current maintainer.

Run `python3 tools/check-license-headers.py` to validate repository governance.
See `.agent/exec-plan-consolidate-tooling-repositories.md` for the migration
record and `CONTRIBUTING.md` for local validation guidance.
