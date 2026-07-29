<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->
# Unified tooling and preview evidence — Plan 08C

This continuation file records Plan 08C evidence after the historical journal
reached the text-file size limit. Full command output remains outside the repo.

- 2026-07-29: Plan 08C added an immutable version-1 JDK catalog with bundled,
  file, and in-memory test sources. It records concrete Temurin 17.0.16+8
  archives and SHA-256 values for macOS ARM64/x64, Linux x64, and Windows x64.
  The new catalog installer uses the shared atomic store, rejects traversal and
  incomplete layouts, probes the declared JAVA_HOME before completion, records
  catalog identity, and reuses completed installations offline. The focused
  tooling-core suite passed. On this macOS ARM64 host, the 176 MB archive was
  downloaded from the concrete release URL, matched
  `f9845abc8403f1d489402201064e7b9f2c57605d8717b85a95a15d94f882eeb7`,
  contained the declared `Contents/Home/bin/java` and `javac`, installed into a
  clean temporary store, passed capability probes, and then reused offline.
