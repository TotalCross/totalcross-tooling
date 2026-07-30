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
- 2026-07-30: all eight catalog URLs returned HTTP 200. On the supported local
  macOS ARM64 host, Temurin 11.0.28+6 and 17.0.16+8 archives were downloaded,
  matched their committed SHA-256 values, contained the declared
  `Contents/Home`, and passed `java -version` plus `javac -version`. The six
  non-local host entries still require matching-host byte and capability
  verification before the catalog gate can close.
- 2026-07-30: the production `JdkCatalogInstaller` installed both verified
  macOS ARM64 entries from the catalog and reused them with `offline=true`
  without a download. The capability probe accepted both homes and recorded
  the expected `jdk-catalog.properties` identities.
