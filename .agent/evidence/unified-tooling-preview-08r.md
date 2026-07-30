<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Plan 08R evidence

- 2026-07-30: candidate release versions are `7.2.3-beta.1` for the SDK,
  `0.1.0-beta.1` for tooling and the Gradle plugin, `2.0.4-beta.1` for the
  Maven plugin, and `0.1.0-beta.1` for the VSIX. The planning manifest records
  exact feature commits and the catalog resource SHA-256.
- 2026-07-30: the Gradle plugin test suite passed with
  `releaseMode=true`, `pluginVersion=0.1.0-beta.1`, `toolingVersion=0.1.0-beta.1`,
  and a local staging repository; this path excludes `mavenLocal()`.
- 2026-07-30: Maven default packaging passed. Its effective POM resolved
  `2.0.4-beta.1` and tooling `0.1.0-beta.1` through the opt-in staging profile.
  License/provenance validation and its 20 tests also passed.
