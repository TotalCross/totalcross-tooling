<!--
Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->

# Contributing

Read `AGENTS.md` and the applicable project instructions before changing code.
Keep each plugin's build and release process independent during this migration.

Run focused project checks first, then run repository governance checks:

    python3 tools/check-license-headers.py
    python3 -m unittest discover -s tests/license_headers -p 'test_*.py'

Preserve historical authorship, third-party notices, and separately licensed
material. Do not add obsolete contact email addresses to current content.

When changing migration metadata, also run:

    python3 tools/check-imported-history.py --source-root ../tooling-migration/source
