<!--
Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->

# Align copyright headers with file creation years

This ExecPlan is a living document and is maintained under `.agent/PLANS.md`.

## Purpose / Big Picture

The repository will validate every applicable imported first-party file against one
creation-year rule. Files created through 2021 will show the TotalCross period
through 2021 and the Amalgam period from 2022 through the current year. Files
created from 2022 will show only Amalgam from their creation year through the
current year. A range whose endpoints are equal is rendered as one year.

## Progress

- [x] (2026-07-16 21:48Z) Read the root and nested agent instructions, the complete ExecPlan requirements, the existing provenance manifest, validator, applier, and tests.
- [x] (2026-07-16 21:48Z) Identified that the existing policy uses first substantive post-2021 changes, leaving historical files without the required 2022 Amalgam line.
- [x] (2026-07-16 21:52Z) Replaced the policy calculation, regression tests, Gradle validator entry point, and contributor instructions.
- [x] (2026-07-16 21:52Z) Rewrote the 104 applicable manifest records and applied the revised headers to the Maven and VS Code imports; all 40 records created through 2021 now have the required 2022-2026 Amalgam line.
- [x] (2026-07-16 21:52Z) Ran focused and repository-wide validation, inspected the diff, and finalized this report.

## Surprises & Discoveries

- Observation: The previous manifest has historical files with no Amalgam line and some with a later start year such as 2026.
  Evidence: `migration/license-provenance.json` records `maven-plugin/package.sh` with only a 2019-2021 TotalCross line and `vscode-extension/src/creator.ts` with an Amalgam line beginning in 2021.

- Observation: The Gradle plugin's legacy entry point performed a separate validation only when invoked from its own directory and rejected the repository's `Copyright (C)` convention elsewhere.
  Evidence: Running `python3 gradle-plugin/tools/check-license-headers.py` from the monorepo root emitted `missing SPDX-FileCopyrightText` diagnostics for valid headers. It now delegates to `tools/check-license-headers.py` unconditionally when the monorepo validator is available.

## Decision Log

- Decision: Use `introduction_year` in the committed provenance manifest as the creation year, and calculate the required copyright lines from that year alone.
  Rationale: It is the per-file Git history evidence already captured for the import and exactly matches the requested rule; post-2021 edit history must not alter the header's Amalgam start year.
  Date/Author: 2026-07-16 / Fabio Sobral requirement

- Decision: Treat the current period as Apache-2.0 for every applicable historical file after adding the Amalgam line.
  Rationale: The repository's existing mixed-period policy already uses Apache-2.0 whenever an Amalgam line is present. Although unchanged historical files normally retain their prior header, the license migration intentionally applies the current license header to every applicable imported file.
  Date/Author: 2026-07-16 / execution record

## Outcomes & Retrospective

The creation-year rule now controls both the manifest and the headers. The
historical files received the Amalgam 2022-2026 line as the deliberate license
migration exception described by the maintainer. Files created in 2026 retain
their single-year Amalgam line. The scope remained governance-only; no product
code, build logic, or historical Git commits were rewritten.

## Editorial Report

### Editorial Summary

The previous validator based the current-period line on substantive changes
after 2021, so some old files had no Amalgam copyright line and others began it
in an arbitrary later year. The repository now derives headers solely from each
file's recorded creation year and applies the current license migration to all
applicable historical files.

### Original Plan versus Actual Outcome

The plan intended to replace the post-2021-change rule, update every applicable
historical header, and validate the result. All intended work was delivered.
During validation, the Gradle subproject entry point was also aligned with the
central validator because it still enforced a superseded header format.

### What Changed

`tools/check-license-headers.py` computes mandatory lines from
`introduction_year` and rejects manifest drift. `tools/build-license-provenance.py`
generates the same values. `migration/license-provenance.json` records the new
expectations, and `tools/apply-license-headers.py` is idempotent for
header-only files. Applicable historical Maven and VS Code files now include
the 2022-2026 Amalgam line. The Gradle wrapper command delegates to the root
validator, and its test verifies that integration.

### Decisions and Trade-offs

The historical start year remains file-specific, while the Amalgam start is
always 2022 for files created through 2021. This intentionally differs from
the ordinary practice of changing headers only when source changes: the
repository-wide license migration justifies updating every applicable file.
The manifest's first-post-2021 field remains as historical audit data but has
no influence on the header rule.

### Unexpected Problems and Discoveries

The header applier added a trailing blank line to a header-only shell file,
which `git diff --check` rejected. It now avoids the separator for empty file
bodies and compares against the original text so repeated runs accurately
report zero changes. The separate Gradle validator also required delegation to
avoid its obsolete SPDX-FileCopyrightText policy.

### Validation and Measurable Results

Observed on 2026-07-16:

- `python3 tests/license_headers/test_check_license_headers.py` passed 17 tests.
- `python3 gradle-plugin/tests/license_headers/test_check_license_headers.py` passed 1 test.
- `python3 -m unittest vscode-extension/tests/test_repository_governance.py` passed 17 tests.
- Root, Gradle, and VS Code validator commands each reported `governance baseline is valid`.
- The manifest contains 104 applicable and 24 explicitly excluded records; all 40 applicable records created through 2021 have the exact 2022-2026 Amalgam line.
- `git diff --check` passed. No performance or size measurement was taken.

### Useful Evidence and Examples

The central acceptance command is `python3 tools/check-license-headers.py`.
`maven-plugin/package.sh` demonstrates a hash-comment historical header, and
`vscode-extension/src/creator.ts` demonstrates a C-style historical header.
The focused tests encode missing-line, wrong-start-year, single-year, shebang,
and XML placement cases.

### Limitations, Remaining Work, and Open Questions

The manifest covers imported project paths and retains explicit exclusions for
third-party, generated, binary, JSON, license, and wrapper material where a
comment header is unsafe or inappropriate. A future calendar year requires
updating committed headers and manifest expectations, although the validator's
calculation itself uses the then-current UTC year.

### Possible Article Angles

- For maintainers of migrated repositories: "Making copyright headers auditable after a license migration" — use per-file Git introduction years rather than repository-wide dates.
- For build-tool maintainers: "One validator, three projects" — prevent nested command entry points from silently applying conflicting governance policies.

### Suggested Narrative

Describe the mismatch between edit-based and creation-based classifications,
show the per-file provenance manifest, explain why a license migration warrants
updating otherwise unchanged files, then demonstrate the central validator and
its delegated project commands. Close with the explicit exclusions and annual
header-refresh requirement.

### Claims Requiring Human Review

The ownership transition and the decision to apply Apache-2.0 to all applicable
historical imported files are legal and historical statements requiring normal
maintainer review.

## Context and Orientation

`migration/license-provenance.json` stores the imported projects' per-file
creation year and expected header. `tools/check-license-headers.py` validates
that manifest and the current files; `tools/apply-license-headers.py` safely
replaces only existing SPDX headers for non-excluded manifest records. The
central tests are in `tests/license_headers/test_check_license_headers.py`.

## Plan of Work

Replace the former substantive-change rule in the provenance builder and central
validator with a creation-year calculation. Make the validator also reject a
manifest record whose expected values drift from that calculation. Update its
regression tests and contributor instructions. Rewrite the manifest
mechanically, run the existing applier for the Maven and VS Code projects, then
run the focused tests and full root validator.

## Concrete Steps

From the repository root, update the policy and tests, refresh the manifest
from its stored `introduction_year` values, and run:

    python3 tests/license_headers/test_check_license_headers.py
    python3 tools/apply-license-headers.py --project maven-plugin
    python3 tools/apply-license-headers.py --project vscode-extension
    python3 tools/check-license-headers.py
    git diff --check

The validator must report `governance baseline is valid` and the tests must
exit zero.

## Validation and Acceptance

Acceptance is a clean validator result for every applicable manifest record,
including historical files that now contain both required copyright lines, and
a passing focused regression suite that rejects an omitted or incorrectly dated
Amalgam line.

## Idempotence and Recovery

The manifest rewrite and header applier are idempotent. If a result is wrong,
correct the calculation or manifest and rerun the applier; it replaces only a
recognized SPDX header and skips explicit exclusions.

## Artifacts and Notes

The final diff and command output provide the durable evidence. No build
artifacts are produced by this governance-only change.

## Interfaces and Dependencies

The validator continues to expose `validate(root, project=None)` and
`validate_record(root, record)`. The manifest retains `introduction_year`,
`expected_copyright_lines`, and `expected_license`; the latter two are now
checked against the creation-year rule rather than trusted as independent
policy input.

Revision 2026-07-16: created to replace the post-2021 substantive-change
classification with the maintainer-requested creation-year rule. Updated after
implementation with the migration rationale, Gradle delegation correction, and
observed validation results.
