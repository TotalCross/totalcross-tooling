<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Plan 08R evidence

- 2026-07-30: candidate release versions are `7.2.3` for the SDK,
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
- 2026-07-30: local release branches `release/7.2.3-beta.1` (SDK) and
  `release/0.1.0-beta.1` (tooling) were created. The SDK branch commit
  `4b6bb643d` sets the numeric SDK line to 7.2.3; its compileJava check passed.
- 2026-07-30: the SDK release branch commit `cbee19888` parameterized its
  Maven repository with `-PstagingRepo`. Publishing all six SDK publications
  to an isolated file repository succeeded with 95 files and no `SNAPSHOT`
  matches.
- 2026-07-30: local file staging succeeded for tooling (125 files), the
  Gradle plugin (40 files), and the Maven plugin (12 files). The release POM
  fix in `8c1f1fe9` pins the candidate defaults to `2.0.4-beta.1` and
  `0.1.0-beta.1`; the staged Maven POM now has zero `SNAPSHOT` matches.
  Release-mode Maven packaging and deployment resolve tooling from the local
  staging repository. Plain packaging without that profile is intentionally
  not a public-consumption test because the beta is not published to Central.
- 2026-07-30: representative staged artifact SHA-256 values were recorded in
  the release manifest for the SDK aggregate, tooling core and CLI bundle,
  Gradle plugin, Maven plugin, and VSIX. Public publication remains disabled.
- 2026-07-30: Gradle plugin tests passed with a new `GRADLE_USER_HOME` and
  tooling resolved from the local file staging repository. Maven release-mode
  packaging also passed with a new `maven.repo.local` and the same staging
  repository.
- 2026-07-30: SDK dependency resolution from a new Maven repository resolved
  `totalcross-sdk:7.2.3` when the already-published
  `maven.totalcross.com/artifactory/repo1` repository was supplied for
  `totalcross-annotations:1.0.0`. A Central-only attempt failed on that
  external dependency; this is recorded as a supported repository requirement,
  not hidden by copying it into the SDK release artifact.
- 2026-07-30: the six non-local catalog archives were downloaded from their
  committed URLs and all matched the catalog SHA-256 values: macOS x64
  Temurin 11/17 (187,862,376 / 180,154,703 bytes), Linux x64 Temurin 11/17
  (195,406,004 / 192,062,472 bytes), and Windows x64 Temurin 11/17
  (199,277,242 / 190,520,081 bytes). Tar/ZIP listings contain every declared
  `JAVA_HOME` entry. Matching-host capability probes remain unavailable here;
  macOS ARM64 probes are the capability evidence currently available.
