<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Plan 08D evidence

- 2026-07-30: Plan 08D final validation passed. The tooling-java full suite,
  project-conversion and CLI focused suites, Gradle plugin tests, VS Code
  compilation, governance tests, license/provenance tests, and `git diff --check`
  passed. The packaged VSIX contained only runtime content and its unpacked
  installed payload ran the extension-host suite with 36 passing tests,
  including the bundled conversion companion executing analyze, apply, validate,
  and rollback behavior against a temporary legacy project.
- 2026-07-30: the official VSIX helper was separately stopped after its dependency
  installer blocked on external Java Pack signature/download work. No source
  failure was observed; the equivalent unpacked release payload passed the same
  host suite.
- 2026-07-30: the license validator now ignores only provenance records whose
  paths are no longer tracked, while still failing for any current tracked file
  without provenance. Its 20-test suite and the repository governance check
  passed. Tooling HEAD is `27e807848ed6ac3345730958d7c635a14fe0ef15`, origin is
  `718eb450ca5e05099dafb1b59d0ccde5378642b2`, and TotalCross local and origin
  are both `9a36178ef185cc3986a446e7cdefdcb0451c402d`.
