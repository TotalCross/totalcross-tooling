<!--
Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->

# Contributing

Use the versioned Gradle wrapper from the repository root. Run the focused
checks before proposing a change:

    python3 tools/check-license-headers.py
    python3 tests/license_headers/test_check_license_headers.py
    ./gradlew test --console=plain

Run `./gradlew check --console=plain` for the full Gradle verification suite.
The optional `./gradlew sdkSourceNetworkTest --console=plain` reaches GitHub and
S3 but does not download a complete SDK archive.

New or changed first-party files must contain these exact SPDX fields in a
comment syntax supported by their format:

    SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
    SPDX-License-Identifier: Apache-2.0

Keep a shebang or XML declaration in its mandatory first position and place the
header immediately after it. Do not add or replace notices in generated,
vendored, imported, or separately licensed files. The Gradle Wrapper files are
third-party generated material and are excluded from the header check.

Inspect `git status --short` before and after a change. Keep commits focused and
descriptive. When importing an existing tree into new history, use only current
evidence to form commit groups; do not invent chronology or motivation.

For substantial work, read `.agent/PLANS.md` and create or update a living
ExecPlan in `.agent/` before implementation.
