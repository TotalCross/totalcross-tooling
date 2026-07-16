<!--
Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->

# Consolidate the TotalCross Maven, VS Code, and Gradle tooling repositories while preserving history and correcting governance

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, `Outcomes & Retrospective`, and `Editorial Report` must be kept up to date as work proceeds.

This plan must be maintained in accordance with `.agent/PLANS.md` in the new `TotalCross/totalcross-tooling` repository. The first commit creates that file and establishes the planning rules under which the remainder of this plan is executed.

## Purpose / Big Picture

TotalCross currently maintains three related developer-tooling projects in separate repositories:

- `https://github.com/TotalCross/totalcross-maven-plugin`
- `https://github.com/TotalCross/totalcross-vscode-plugin`
- `https://github.com/TotalCross/totalcross-gradle-plugin`

The goal is to consolidate them into the already-created, empty repository:

- `https://github.com/TotalCross/totalcross-tooling`

The resulting repository is a polyglot tooling monorepo. Each imported project remains independently buildable, versioned, and releasable, but common governance, licensing validation, documentation, and future shared tooling can be maintained together.

The final top-level layout must be:

    totalcross-tooling/
    ├── .agent/
    │   ├── PLANS.md
    │   └── exec-plan-consolidate-tooling-repositories.md
    ├── .github/
    │   ├── CODEOWNERS
    │   └── workflows/
    ├── maven-plugin/
    ├── vscode-extension/
    ├── gradle-plugin/
    ├── tools/
    ├── tests/
    ├── AGENTS.md
    ├── AUTHORS.md
    ├── CONTRIBUTING.md
    ├── LICENSE
    ├── NOTICE
    └── README.md

The directory names deliberately omit the redundant `totalcross-` prefix.

If a future history rewrite is performed, it must preserve original contributors and commit identities. Moving files into subdirectories necessarily changes commit object IDs when `git filter-repo` rewrites each tree, but it must not replace original authors, committers, author dates, committer dates, messages, or merge relationships. The corresponding audit inputs and results must be retained and validated. The current `main` state does not claim that this future audit has occurred.

After completion, a developer must be able to:

1. browse each imported project's complete rewritten history under its final subdirectory;
2. run `git blame` and see the original contributors rather than a migration account;
3. see Fabio Sobral (`@flsobral`) identified as the sole current maintainer and default code owner;
4. understand the original creator and historical contributors of each project without obsolete contact email addresses;
5. run one root governance command that verifies license headers, provenance years, project attribution, exclusions, and repository-level policy;
6. build and test each plugin with its existing toolchain from its new directory;
7. follow links from the archived source repositories to the corresponding location in `totalcross-tooling`.

## Preliminary verified context

The following facts describe the repository state reviewed on 2026-07-16:

- `main` and `origin/main` now include the execution commits produced by this plan; the reviewed release baseline was `0c337b7`, and the current head is recorded by `git log -1`.
- The three projects are present under `maven-plugin/`, `vscode-extension/`, and `gradle-plugin/`.
- The root contains Apache-2.0 governance, the provenance manifest, license validators, and path-aware workflows.
- The Maven plugin currently has no top-level `LICENSE` file in its default branch.
- The VS Code project currently has an Apache-2.0 root license and a `NOTICE` describing an earlier MIT period.
- The VS Code project's current governance identifies Italo Yeltsin (`@ItaloYeltsin`) as original creator and Fabio Sobral (`@flsobral`) as sole current maintainer.
- The Gradle plugin currently uses Apache-2.0 and identifies Fabio Sobral as creator and maintainer.
- The VS Code source history begins in 2019, and its original MIT period is recorded in the project governance files.
- The current VS Code header policy includes historical headers such as `Copyright (C) 2020-2021 TotalCross Global Mobile Platform Ltda.`. That blanket start year is not sufficient evidence for all files because the repository began in 2019. It may be correct for a particular file created in 2020, but it is wrong for any applicable file introduced in 2019.
- `migration/license-provenance.json` contains 128 records: 16 Maven, 74 VS Code, and 38 Gradle.
- The root license validator passes, and the current regression suite contains and passes 17 tests.
- No committed history-audit input set is present in `main`.

The last point is a required correction, not merely a possible follow-up. The migration must calculate the initial year independently for every applicable file. Do not change all VS Code files to 2019, and do not retain all of them as 2020. Use each file's own history.

## Progress

Use UTC timestamps on every completed entry. Split partially completed entries into explicit completed and remaining portions at every stopping point.

- [x] (2026-07-16 20:44Z) Record source and target branch heads, visibility, refs, contributor summaries, and clean source working trees under `../tooling-migration/evidence/before/`; `origin/work` was explicitly excluded.
- [x] (2026-07-16 20:44Z) Create and fsck local mirrors plus disposable-clone backup refs for all imports.
- [x] (2026-07-16 20:44Z) Inventory source governance, licenses, attribution, contacts, CI, build entry points, wrappers, and generated material.
- [x] (2026-07-16 20:44Z) Generate pre-rewrite provenance records for all 128 source paths.
- [x] (2026-07-16 20:44Z) Generate `migration/vscode-year-report.md` and correct VS Code headers per file rather than by blanket year.
- [x] (2026-07-16 20:44Z) Determine original creators from attribution and first commits.
- [x] (2026-07-16 20:44Z) Record historical contributors without changing Git identities.
- [x] (2026-07-16 20:44Z) Document Maven's absent authoritative source license, VS Code's MIT period, and Gradle's Apache-2.0 origin.
- [x] (2026-07-16) Establish the current monorepo layout, root governance files, and independent project directories on `main`.
- [x] (2026-07-16) Generate and commit `migration/license-provenance.json` for 128 source paths and `migration/vscode-year-report.md` for the VS Code year review.
- [x] (2026-07-16) Normalize current-tree headers, attribution, notices, and repository links according to the provenance policy.
- [x] (2026-07-16) Add path-aware CI without coupling the three project versions or build systems.
- [x] (2026-07-16) Run the root validator and its current 17 regression tests; both pass on the reviewed working tree.
- [x] (2026-07-16) Audit current `main` lineage against the read-only source mirrors; all 157 source commits are located, but exact preservation fails for 68 VS Code committer tuples, 6 Gradle committer tuples, and 3 project-boundary parent relationships. The result is recorded in `migration/history-audit-report.md`; no history rewrite was attempted.
- [x] (2026-07-16) Validate an independent clean clone after the Maven late-history refresh: root and VS Code governance passed, Maven package passed with tests skipped, Gradle test/publication passed, and VS Code audit/compile/integration tests passed.
- [x] (2026-07-16) Confirm repository-side VS Code 0.0.16 publication metadata: version `0.0.16`, tag `v0.0.16`, publication workflow, and monorepo repository URL are present.
- [x] (2026-07-16) Confirm the public VS Code Marketplace item responds successfully and exposes version `0.0.16` with current monorepo links; older versions remain historical entries.
- [x] (2026-07-16) Validate local Maven and Gradle publication metadata: Maven package `com.totalcross:totalcross-maven-plugin:2.0.3` succeeds with tests skipped; Gradle tests and `publishToMavenLocal` succeed for the plugin markers and `com.totalcross:totalcross-gradle-plugin:0.1.0-SNAPSHOT`.
- [x] (2026-07-16) Validate repository links, open-work redirects, target release metadata, VS Code Marketplace 0.0.16 metadata, and the absence of configured external Maven/Gradle registry publication in the checked-in build descriptors.
- [x] (2026-07-16) Archive `TotalCross/totalcross-maven-plugin`, `TotalCross/totalcross-vscode-plugin`, and `TotalCross/totalcross-gradle-plugin` after verifying backups, move notices, target publication, and redirected open work.
- [x] (2026-07-16) Reconcile this plan with the observed `main` state and record the remaining validation gaps.

## Surprises & Discoveries

Record repository-specific findings here as implementation proceeds. Do not silently resolve licensing, authorship, or historical ambiguity.

- Observation: The VS Code repository began in 2019, but its current historical source-header convention uses a generic `2020-2021` range.
  Evidence: The source history begins in 2019 and contains the original 2019 MIT copyright period. Current files such as `src/extension.ts` use `2020-2021`. This proves that a repository-wide hard-coded start year is unsafe; it does not by itself prove the correct year for any individual source file.

- Observation: The current `main` history does not contain the governance-first import topology described by the original version of this plan.
  Evidence: `main` and `origin/main` point to `0c337b7`; the first-parent history begins with the existing project history. The current plan therefore treats the observed `main` tree as authoritative and does not claim the earlier draft's import sequence.

- Observation: Maven has no authoritative `LICENSE` or `COPYING` in its default branch.
  Evidence: the source inventory listed only `README.md`; GitHub reported `licenseInfo: null`. The imported project receives Apache-2.0 prospectively and `maven-plugin/NOTICE` records the uncertainty.

- Observation: Maven's JDK download unit test is not portable to this macOS environment.
  Evidence: both candidate and clean-clone `mvn test` downloaded 301,209,836 bytes and then failed `JavaJDKManagerTest.downloadAndUnzip` at line 46 because its expected JDK directory did not exist. `mvn -DskipTests package` passed.

- Observation: The current VS Code integration runner does not configure a short temporary user-data directory.
  Evidence: `vscode-extension/src/test/runTest.ts` calls `runTests` with only `extensionDevelopmentPath` and `extensionTestsPath`. The current local and independent `/tmp` clean-clone runs passed 20 integration tests without reproducing the earlier path-length failure. No workaround was imported; revisit only if a different clean environment reproduces the failure.

- Observation: The current `main` contains the source histories but does not preserve every original history object exactly.
  Evidence: `migration/history-audit-report.md` located 83 Maven, 68 VS Code, and 6 Gradle source commits by author metadata, complete message, and changed paths. All 68 VS Code and all 6 Gradle commits differ in committer metadata, and three project-boundary parent relationships differ.

- Observation: The nested VS Code governance command had stale blanket-year and MIT expectations.
  Evidence: Before the fix it failed against current first-party headers; after delegating repository validation to `tools/check-license-headers.py --project vscode-extension`, the command and its 17 unit tests passed.

- Observation: the first Gradle test invocation lost its command wrapper before reporting completion, although its worker completed normally.
  Evidence: `gradle-plugin/build/test-results/test/` contains four XML reports with 18 tests and zero failures; a separate `./gradlew publishToMavenLocal --console=plain --no-daemon` reported `BUILD SUCCESSFUL`.

- Observation: Maven `master` advanced from the snapshot during the migration window.
  Evidence: The current `maven-plugin/pom.xml` has the expected 2022 Amalgam provenance line. `migration/history-audit-report.md` records the source revision and the current-main comparison; it does not claim a filtered-history rewrite.

- Observation: the imported project READMEs contain move notices, but the original-repository publication is external to this repository.
  Evidence: `maven-plugin/README.md`, `vscode-extension/README.md`, and `gradle-plugin/README.md` begin with links to their corresponding monorepo directories. The original-repository commit IDs are not part of `main` and are not used as evidence in this plan.

- Observation: the VS Code 0.0.16 publication is visible and its current metadata points to the monorepo.
  Evidence: `vscode-extension/package.json` contains version `0.0.16` and the `totalcross-tooling/tree/main/vscode-extension` repository URL. On 2026-07-16, [the Marketplace item page](https://marketplace.visualstudio.com/items?itemName=TotalCross.vscode-totalcross) returned HTTP 200, displayed version `0.0.16`, and contained links to `TotalCross/totalcross-tooling`; older versions such as 0.0.15 remain in version history.

- Observation: open work in the source repositories was redirected without being closed.
  Evidence: comments point to the monorepo on Maven PRs #26 and #25, Maven issues #22 and #19, and VS Code issue #11. Keeping them open preserves their historical discussion and avoids misrepresenting unresolved work as completed.

- Observation: The original repositories are now archived rather than deleted.
  Evidence: GitHub reports `isArchived: true` for all three source repositories after the target publication, move-notice, backup, and open-work checks completed.

Add discoveries such as:

- a file was copied into the repository and its Git introduction date differs from its actual upstream origin;
- a rename cannot be followed automatically and needs a provenance override;
- a file contains both pre-2022 and post-2021 substantive work;
- a source repository's current license differs from its original license;
- a repository lacks an explicit license or contains conflicting package metadata;
- a named maintainer was actually a contributor rather than an original creator;
- an email appears in current first-party content, package metadata, generated artifacts, historical Git metadata, or third-party text;
- a tag name collides with another project's tag;
- a merge or signed commit cannot be preserved exactly after path rewriting;
- a build assumes the repository root rather than the plugin subdirectory;
- a CI workflow or publication task contains an old repository URL;
- a generated file must be fixed through its generator rather than edited directly.

For every discovery, record concise evidence: repository-relative paths, original commit IDs, commands, output excerpts, and the chosen treatment.

## Decision Log

- Decision: Create a new neutral monorepo rather than rename one of the source repositories.
  Rationale: No single plugin is the sole predecessor of the combined tooling repository. A new repository avoids favoring one project and permits a governance commit before all imports.
  Date/Author: 2026-07-16 / Fabio Sobral requirement

- Decision: Use the exact final subdirectories `maven-plugin/`, `vscode-extension/`, and `gradle-plugin/` in that import order.
  Rationale: These names are concise inside `totalcross-tooling` and match the requested ordering without redundant `totalcross-` prefixes.
  Date/Author: 2026-07-16 / Fabio Sobral requirement

- Decision: Keep independent builds, versions, changelogs, releases, and publication pipelines initially.
  Rationale: Maven, VS Code, and Gradle tooling have different ecosystems and release cadences. Repository consolidation must not manufacture a single version or build system.
  Date/Author: 2026-07-16 / OpenAI

- Decision: Treat the checked-out `main` tree and history as authoritative for this revision of the plan.
  Rationale: The candidate migration history described by the earlier plan is not part of `main`; retaining its commit identifiers would make this document claim work that cannot be observed from the published branch.
  Date/Author: 2026-07-16 / Fabio Sobral requirement

- Decision: Preserve original Git identity metadata and do not replace author or committer fields with Fabio Sobral or the migration operator.
  Rationale: Current maintenance is repository governance, not retroactive authorship. Contributor recognition depends on preserving original names, emails, and dates.
  Date/Author: 2026-07-16 / Fabio Sobral requirement

- Decision: Removing contact email applies to the current working tree and current metadata, not historical commit author emails.
  Rationale: Rewriting author emails would damage contributor attribution and is not equivalent to removing an obsolete public contact address. Historical Git identity data remains intact unless a separate privacy/legal instruction explicitly requires rewriting it.
  Date/Author: 2026-07-16 / OpenAI

- Decision: Namespace imported tags as `maven-plugin-*`, `vscode-extension-*`, and `gradle-plugin-*`.
  Rationale: Common tags such as `v1.0.0` can collide in a monorepo. Namespaced tags remain understandable and independently releasable.
  Date/Author: 2026-07-16 / OpenAI

- Decision: Do not claim a cross-repository history audit without the required source mirrors and mapping inputs supplied explicitly.
  Rationale: `tools/check-imported-history.py` compares original and rewritten commit metadata; it cannot establish those facts from the current tree alone. The script remains relevant as an optional audit tool for a future, separately evidenced history migration, but its result is not part of the current `main` acceptance evidence.
  Date/Author: 2026-07-16 / OpenAI

- Decision: Do not import a VS Code integration-runner portability workaround after the current clean-clone runs passed.
  Rationale: The failure was not reproduced in the current environment, so adding a workaround would broaden the change without demonstrated benefit.
  Date/Author: 2026-07-16 / OpenAI

- Decision: Preserve the current `main` history and document exact-preservation failures instead of rewriting it.
  Rationale: Repairing committer metadata and project-boundary parents would require prohibited history rewriting and force-pushing. The audit result is more trustworthy when the observed history remains unchanged.
  Date/Author: 2026-07-16 / OpenAI

- Decision: Archive the three source repositories after publishing the target and verifying redirects, backups, and open-work comments.
  Rationale: The repositories retain their Git history, releases, issues, and pull requests while new development is directed to the validated monorepo. Archiving is reversible and does not delete historical resources.
  Date/Author: 2026-07-16 / Fabio Sobral requirement

- Decision: Use Apache License 2.0 as the root license for current monorepo work.
  Rationale: This is the explicitly requested global license. Historical grants and original project licenses remain documented and preserved where applicable.
  Date/Author: 2026-07-16 / Fabio Sobral requirement

- Decision: Treat a source repository with no authoritative license as Apache-2.0 in the imported monorepo, but do not claim it was historically published under Apache-2.0 before the migration.
  Rationale: The requested fallback supplies clear terms going forward without inventing a historical license. `NOTICE` must describe that Apache-2.0 was assigned at migration time.
  Date/Author: 2026-07-16 / Fabio Sobral requirement

- Decision: Calculate copyright start years per file from pre-migration source history.
  Rationale: A repository-wide start year is demonstrably unsafe in the VS Code project. The exact year must reflect the file's own introduction or the first contribution period attributable to a holder.
  Date/Author: 2026-07-16 / Fabio Sobral clarification

- Decision: Interpret “original date” separately for each copyright holder.
  Rationale: For TotalCross, the start year is the file's actual introduction year when that year is 2021 or earlier. For Amalgam, the start year is the first substantive post-2021 change attributable to the current period, or the creation year for a file introduced in 2022 or later. Using a 2019 start for Amalgam merely because an older file was created in 2019 would falsely imply Amalgam ownership during the TotalCross period.
  Date/Author: 2026-07-16 / OpenAI

- Decision: Do not count path rewriting, merge commits, header-only normalization, formatting-only migration edits, or move notices as substantive post-2021 product changes.
  Rationale: Otherwise every imported historical file would receive an Amalgam line solely because it was migrated in 2026, defeating provenance-based classification.
  Date/Author: 2026-07-16 / OpenAI

- Decision: Apply copyright and license corrections in dedicated post-import commits rather than changing the original author of historical commits.
  Rationale: The final tree becomes compliant while historical contribution identity remains truthful. Path rewriting already changes object IDs; additional content rewriting of every historical commit is unnecessary for the requested final-state validation and would increase legal and operational risk.
  Date/Author: 2026-07-16 / OpenAI

- Decision: Preserve third-party, vendored, generated, copied, and separately licensed material unless authoritative evidence permits a change.
  Rationale: Monorepo policy cannot override third-party ownership or license notices.
  Date/Author: 2026-07-16 / OpenAI

- Decision: Fabio Sobral (`@flsobral`) is the sole current maintainer and default code owner, but not automatically the original creator of every imported project.
  Rationale: Creation, contribution, maintenance, and copyright ownership are distinct roles.
  Date/Author: 2026-07-16 / Fabio Sobral requirement

- Decision: Preserve previously named contributors in a `Historical contributors` section and keep Git history as the authoritative complete contribution record.
  Rationale: Updating current maintenance must not erase legitimate earlier contributors or imply that a manually maintained list is exhaustive.
  Date/Author: 2026-07-16 / Fabio Sobral requirement

- Decision: Update the original repositories only after the monorepo passes validation.
  Rationale: Move notices must not send users to an incomplete or invalid destination.
  Date/Author: 2026-07-16 / OpenAI

## Copyright and license classification

The migration must produce a provenance manifest before any migration-only edit. Create `migration/license-provenance.json` or an equivalently readable, deterministic file. It must record at minimum:

- source repository;
- original path;
- final monorepo path;
- file introduction commit and year;
- whether rename following was automatic or manually overridden;
- first substantive change year at or after 2022, when one exists;
- original repository license;
- current file license;
- expected copyright lines;
- expected SPDX identifier;
- classification reason;
- exclusion reason, when excluded.

Use the following rules for first-party files whose format safely supports comments.

### Historical file created no later than 2021 and not substantively changed after 2021

Use the actual introduction year, not a repository-wide default:

    Copyright (C) <creation-year>-2021 TotalCross Global Mobile Platform Ltda.
    SPDX-License-Identifier: <original-repository-license>

If `<creation-year>` is 2021, use a single year rather than `2021-2021`.

For example, a VS Code file introduced in 2019 and not substantively changed after 2021 must use:

    Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.
    SPDX-License-Identifier: MIT

A VS Code file introduced in 2020 and not substantively changed after 2021 must use:

    Copyright (C) 2020-2021 TotalCross Global Mobile Platform Ltda.
    SPDX-License-Identifier: MIT

The validator must reject either file if the other file's start year is copied into its header.

### Historical file created no later than 2021 and substantively changed from 2022 onward

Retain the historical holder and add the current-period holder:

    Copyright (C) <creation-year>-2021 TotalCross Global Mobile Platform Ltda.
    Copyright (C) <first-substantive-post-2021-year>-2026 Amalgam Solucoes em TI Ltda.
    SPDX-License-Identifier: Apache-2.0

The root and subproject `NOTICE` material must explain that earlier versions and historical portions may remain available under their original license grants. Do not delete an original MIT notice when it remains legally required.

### File created in 2022 or later

Use:

    Copyright (C) <creation-year>-2026 Amalgam Solucoes em TI Ltda.
    SPDX-License-Identifier: Apache-2.0

If the file was introduced in 2026, use a single year:

    Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
    SPDX-License-Identifier: Apache-2.0

### Repository with no original license

The imported project uses Apache-2.0 from the migration baseline. A first-party file created no later than 2021 still records historical TotalCross ownership, but its SPDX identifier is Apache-2.0 because no authoritative prior file license was found:

    Copyright (C) <creation-year>-2021 TotalCross Global Mobile Platform Ltda.
    SPDX-License-Identifier: Apache-2.0

`NOTICE` must state that the source repository contained no authoritative license and that Apache-2.0 was assigned for the imported project at migration time. Record any conflicting package metadata in `Surprises & Discoveries` and require human review before publication.

### Exclusions

Do not add or replace headers in:

- third-party or vendored source;
- generated files whose generator is authoritative;
- lockfiles;
- binary files;
- minified assets;
- images and media;
- external fixtures copied verbatim;
- license texts;
- patch files;
- formats in which comments would change parsing semantics;
- files explicitly carrying a different authoritative license.

Where a generated source needs a corrected header, update its generator or template and regenerate it. Record exclusions explicitly in the provenance manifest so the validator does not silently ignore arbitrary paths.

## Header syntax

Use the shortest safe syntax while preserving mandatory first lines.

For Java, TypeScript, JavaScript, Groovy, Gradle, and similar C-style files:

    /*
     * Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.
     * SPDX-License-Identifier: MIT
     */

For a mixed-period C-style file:

    /*
     * Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.
     * Copyright (C) 2023-2026 Amalgam Solucoes em TI Ltda.
     * SPDX-License-Identifier: Apache-2.0
     */

For Python, shell, YAML, and similar hash-comment files:

    # Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
    # SPDX-License-Identifier: Apache-2.0

Keep a shebang first:

    #!/usr/bin/env python3
    # Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
    # SPDX-License-Identifier: Apache-2.0

For XML-compatible files, keep an XML declaration first when present:

    <?xml version="1.0" encoding="UTF-8"?>
    <!--
      Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.
      SPDX-License-Identifier: Apache-2.0
    -->

For Markdown governance files, use an HTML comment when the repository policy requires a header. Do not place a header before a required front-matter delimiter.

## Context and Orientation

Work from a parent directory containing separate clones. Do not perform the history rewrite in a developer's only clone.

Recommended workspace:

    tooling-migration/
    ├── source/
    │   ├── totalcross-maven-plugin.git
    │   ├── totalcross-vscode-plugin.git
    │   ├── totalcross-gradle-plugin.git
    │   └── totalcross-tooling.git
    ├── rewrite/
    │   ├── maven-plugin/
    │   ├── vscode-extension/
    │   └── gradle-plugin/
    ├── work/
    │   └── totalcross-tooling/
    └── evidence/

A mirror clone is a bare repository containing all refs. Use it as an immutable local backup and as the source for disposable working clones.

Before editing, install and record versions of:

    git --version
    git filter-repo --version
    gh --version
    python3 --version
    java -version
    mvn -version
    node --version
    npm --version

Use a supported `git-filter-repo` release. Do not use `git filter-branch`.

Read all source `AGENTS.md`, `.agent/PLANS.md`, `CONTRIBUTING.md`, and nested instructions before changing any imported project. Source instructions remain relevant to building and testing that project even after it is nested.

## Milestone 1: Capture immutable source evidence

Create mirror clones and evidence before any mutation. At the end of this milestone, every source ref can be restored and the current public state is documented.

From `tooling-migration/source`:

    git clone --mirror https://github.com/TotalCross/totalcross-maven-plugin.git totalcross-maven-plugin.git
    git clone --mirror https://github.com/TotalCross/totalcross-vscode-plugin.git totalcross-vscode-plugin.git
    git clone --mirror https://github.com/TotalCross/totalcross-gradle-plugin.git totalcross-gradle-plugin.git
    git clone --mirror https://github.com/TotalCross/totalcross-tooling.git totalcross-tooling.git

For each mirror, save:

    git show-ref --head
    git for-each-ref --format='%(refname) %(objectname) %(creatordate:iso8601-strict)'
    git log --all --graph --decorate --oneline
    git shortlog -sne --all
    git rev-list --all --count
    git fsck --full

Also record repository settings and public objects with `gh` where available:

    gh repo view TotalCross/totalcross-maven-plugin --json name,defaultBranchRef,isArchived,licenseInfo,url
    gh repo view TotalCross/totalcross-vscode-plugin --json name,defaultBranchRef,isArchived,licenseInfo,url
    gh repo view TotalCross/totalcross-gradle-plugin --json name,defaultBranchRef,isArchived,licenseInfo,url
    gh repo view TotalCross/totalcross-tooling --json name,defaultBranchRef,isArchived,licenseInfo,url

Record tags, releases, open PRs, issues, Actions, package publication configuration, Marketplace links, and branch protection. Do not assume that Git history alone migrates issues, PRs, releases, stars, forks, discussions, environments, secrets, or repository settings.

Acceptance for this milestone:

- all four mirror clones pass `git fsck --full`;
- all refs and default branch heads are recorded under `evidence/before/`;
- each source commit count and contributor shortlog is saved;
- no source or remote ref has been changed.

## Milestone 2: Inventory licenses, attribution, contacts, and per-file provenance

Create ordinary read-only working clones from the mirrors. Inspect each source repository before deciding its policy.

Run from each source checkout:

    git status --short --branch
    find . -name AGENTS.md -o -name PLANS.md
    find . -maxdepth 4 -type f \( \
      -iname 'LICENSE*' -o \
      -iname 'COPYING*' -o \
      -iname 'NOTICE*' -o \
      -iname 'AUTHORS*' -o \
      -iname 'README*' -o \
      -iname 'CONTRIBUTING*' -o \
      -iname 'CODEOWNERS' \
    \) -print | sort

    git grep -n -I -E \
      'Copyright|SPDX-License-Identifier|SPDX-FileCopyrightText|License|Maintainer|Author|Contributor|Contact|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}' \
      -- . || true

Use more precise email detection in the implementation script; the grep above is only orientation. Search package metadata such as `pom.xml`, `package.json`, Gradle publication blocks, extension manifests, and release scripts for obsolete contacts and repository URLs.

### Determine creators and historical contributors

For each repository:

1. Prefer a clear existing `Original creator` or equivalent statement supported by history.
2. If no project-level creator attribution exists, use the author of the first commit as the original creator, exactly as requested.
3. Keep Fabio Sobral as sole current maintainer regardless of original creator.
4. Build a historical contributor list from existing documentation and `git shortlog -sne --all`.
5. Preserve contributor names even if their email is removed from current documentation.
6. Exclude obvious bots from the human contributor list but leave bot commits unchanged in Git history.
7. Do not claim copyright assignment or employment relationships without authoritative evidence.

Save the first commit evidence with:

    git log --reverse --all --format='%H%x09%aI%x09%an%x09%ae%x09%s' | head -n 20

If multiple roots exist, explain which root is the project origin and why.

### Generate exact file years

Implement a temporary or reusable provenance tool before changing headers. It must process NUL-delimited paths from Git:

    git ls-files -z

For each applicable file, determine the introduction commit and year from the unmodified source history. A useful manual check is:

    git log --follow --diff-filter=A \
      --format='%H%x09%aI%x09%an%x09%ae' \
      -- path/to/file

The implementation must not assume that the first line returned is the oldest entry. Parse the complete output and select the oldest qualifying introduction. When `--follow` cannot resolve a copy or rename, use commit-tree inspection and record a manual override with evidence.

Determine the first substantive post-2021 change from the original source history. Ignore:

- header-only changes;
- copyright/license-only changes;
- repository move notices;
- pure path renames;
- `git filter-repo` path rewriting;
- merge commits without file content changes;
- generated-file refreshes when authorship belongs to the generator and no first-party authored content changed.

For ambiguous mixed changes, record the commit and require human review rather than guessing.

### Mandatory VS Code year validation

The tool must audit every applicable file under the VS Code source repository. Add an explicit report with columns:

    path | introduction commit | introduction year | first post-2021 substantive year | current header | expected header | status

At minimum, prove these cases:

- a file introduced in 2019 is expected to start at 2019;
- a file introduced in 2020 is expected to start at 2020;
- a file introduced in 2021 is expected to use 2021 without a redundant range;
- a file introduced in 2022 or later receives only the Amalgam line;
- a historical file with a substantive post-2021 change receives both holder lines;
- a header-only governance change does not by itself add an Amalgam line.

Do not infer that `src/extension.ts` was created in 2019 merely because the repository was created in 2019. Verify its own introduction commit. The same rule applies to every file.

Acceptance for this milestone:

- every applicable tracked file has a deterministic provenance record or an explicit reviewed exclusion;
- the VS Code report contains no unresolved generic `2020-2021` assumption;
- original licenses and transition points are documented per project;
- current-tree contact emails are inventoried separately from historical Git author emails;
- original creators and historical contributors are supported by evidence.

## Milestone 3: Create the governance-first target commit

Clone the empty target repository into `work/totalcross-tooling`. Confirm that it has no commits or that the remote contains only an intentionally disposable placeholder. If a placeholder exists, stop and record whether it may be removed; do not overwrite an unexplained commit.

Create `main` and add only governance, policy, validation, planning, and repository-orientation files. Do not import plugin source in this commit.

Required files:

- `README.md`
- `LICENSE` containing the canonical Apache License 2.0 text
- `NOTICE`
- `AUTHORS.md`
- `CONTRIBUTING.md`
- `AGENTS.md`
- `.agent/PLANS.md`
- `.agent/exec-plan-consolidate-tooling-repositories.md`
- `.github/CODEOWNERS`
- `.github/workflows/governance-validation.yml`
- `tools/check-license-headers.py`
- `tests/license_headers/test_check_license_headers.py`
- an explicit policy/config file used by the validator, such as `tools/license-policy.json`

`README.md` must explain that this is the shared repository for TotalCross development tooling, list the three planned subprojects in import order, state that builds and releases remain independent initially, identify Fabio Sobral as sole current maintainer, and avoid claims that the projects are already imported before they are.

`AUTHORS.md` must distinguish:

- current sole maintainer: Fabio Sobral (`@flsobral`);
- root monorepo governance creator, if useful;
- per-project original creators, populated after provenance inspection or explicitly marked as pending until import normalization;
- historical contributors, populated per project after imports;
- Git history as the authoritative complete contribution record.

`.github/CODEOWNERS` must contain:

    * @flsobral

Do not enable mandatory review or branch protection as part of this file-only governance commit unless separately requested.

`AGENTS.md` must instruct agents to:

- read `.agent/PLANS.md` and applicable nested instructions;
- keep an active ExecPlan for long work;
- preserve imported authors and history;
- never remove third-party notices;
- never publish obsolete contact emails;
- run focused checks before broad builds;
- keep verbose output in files and summarize it;
- avoid cross-plugin refactoring during migration;
- avoid history rewrite, force-push, tag deletion, or archiving without explicit scope and backups.

The validator in this first commit must already support:

- root Apache-2.0 governance headers;
- per-subdirectory policy entries that can be activated after imports;
- NUL-safe Git path handling;
- exact company spelling;
- exact year and range validation;
- single-year normalization;
- shebang and XML declaration preservation;
- deterministic sorted diagnostics;
- explicit exclusions;
- provenance-manifest validation;
- obsolete current-tree contact detection;
- repository-level attribution checks;
- a mode that succeeds before plugin directories exist.

Add tests for at least:

1. valid current Apache C-style header;
2. valid historical MIT header;
3. valid mixed-period header;
4. incorrect initial year;
5. generic `2020-2021` when provenance requires `2019-2021`;
6. generic `2019-2021` when provenance requires `2020-2021`;
7. redundant `2021-2021` instead of `2021`;
8. missing TotalCross line;
9. missing Amalgam line for a substantive post-2021 file;
10. incorrectly added Amalgam line for a header-only migration;
11. wrong SPDX identifier;
12. missing SPDX identifier;
13. third-party exclusion;
14. generated-file exclusion;
15. path containing spaces;
16. shebang placement;
17. XML declaration placement;
18. obsolete contact email in current content;
19. historical Git email not scanned as current content;
20. deterministic sorted output.

Commit only after:

    python3 tools/check-license-headers.py
    python3 -m unittest discover -s tests/license_headers -p 'test_*.py'
    git diff --check

Use a commit message equivalent to:

    chore(governance): establish tooling repository baseline

Acceptance for this milestone:

- this is the first commit on `main` first-parent history;
- it contains no plugin implementation source;
- the root license is Apache-2.0;
- Fabio Sobral is sole maintainer and default code owner;
- validator and tests pass from the governance-only tree.

## Milestone 4: Rewrite and import the Maven plugin history

Create a disposable clone from the Maven mirror:

    git clone source/totalcross-maven-plugin.git rewrite/maven-plugin
    cd rewrite/maven-plugin
    git checkout master

Create backup refs inside the disposable clone:

    git branch backup/pre-filter-master master
    git tag migration-backup-maven-<short-sha> master

Run `git filter-repo` so all historical files move under `maven-plugin/` and tags are namespaced. Use the exact syntax supported by the installed version; a representative command is:

    git filter-repo --force \
      --to-subdirectory-filter maven-plugin \
      --tag-rename '':'maven-plugin-'

Do not use mailmap callbacks, name callbacks, email callbacks, message callbacks, or date callbacks. The only intended history transformation is the path prefix and tag namespace.

Before merge, validate the filtered history against the mirror:

- same number of commits reachable from the source default branch;
- same author name, author email, author timestamp, committer name, committer email, committer timestamp, and message for every mapped commit;
- same parent count and corresponding mapped parent relationships;
- same number of merge commits;
- no file remains outside `maven-plugin/` in the filtered source branch;
- namespaced tags point to mapped equivalents;
- `git fsck --full` succeeds.

In the target repository:

    git remote add import-maven ../../rewrite/maven-plugin
    git fetch import-maven --tags
    git merge --no-ff --allow-unrelated-histories \
      import-maven/master \
      -m 'chore(import): import Maven plugin history'

The current target branch must be the first parent. Confirm with:

    git show --no-patch --pretty=raw HEAD

Then create a separate Maven normalization commit. It must:

- add or update `maven-plugin/README.md` with original creator, sole current maintainer, historical contributors, current location, local build commands, and license status;
- remove obsolete contact email fields from current first-party documentation and package metadata;
- add `maven-plugin/AUTHORS.md` if useful;
- add `maven-plugin/NOTICE` describing its license provenance;
- add an Apache-2.0 license for this subproject if no authoritative original license exists;
- apply exact provenance-based headers to applicable files;
- preserve third-party notices;
- adapt root-relative scripts or workflows to the subdirectory without broad refactoring;
- add the Maven policy entries to the root provenance manifest and validator configuration.

Use a commit message equivalent to:

    chore(maven): normalize imported governance

Run from `maven-plugin/` the repository's supported tests and package checks. Start with the commands evidenced by the project, typically:

    mvn --batch-mode --no-transfer-progress test
    mvn --batch-mode --no-transfer-progress package

If the historical build requires unavailable services or credentials, separate deterministic unit validation from network or publication checks and document the limitation. Do not hide a failure by removing tests.

## Milestone 5: Rewrite and import the VS Code plugin history

Create and filter a disposable clone:

    git clone source/totalcross-vscode-plugin.git rewrite/vscode-extension
    cd rewrite/vscode-extension
    git checkout master
    git branch backup/pre-filter-master master
    git tag migration-backup-vscode-<short-sha> master
    git filter-repo --force \
      --to-subdirectory-filter vscode-extension \
      --tag-rename '':'vscode-extension-'

Run the same metadata, topology, tag, path, and `git fsck` validations as for Maven.

Merge it second:

    git remote add import-vscode ../../rewrite/vscode-extension
    git fetch import-vscode --tags
    git merge --no-ff --allow-unrelated-histories \
      import-vscode/master \
      -m 'chore(import): import VS Code plugin history'

Then create a dedicated VS Code normalization commit.

### Required VS Code year correction

Use the provenance report generated from the source repository before migration. For every applicable historical file:

1. read its actual introduction year;
2. compare that year with the current header;
3. replace a generic 2020 start when the file was introduced in 2019;
4. retain a 2020 start when the file was actually introduced in 2020;
5. use a single 2021 year when the file was introduced in 2021;
6. add the Amalgam line only when the file was introduced in 2022 or later or had a substantive post-2021 change before migration;
7. ignore the 2026 import, move, and header-normalization commits when classifying substantive changes;
8. retain MIT for untouched historical files when MIT is their original license;
9. use Apache-2.0 for new/current-period files and mixed-period files according to the documented transition;
10. update VS Code validator fixtures and policies so the corrected years cannot regress.

The normalization commit must also:

- retain Italo Yeltsin as original creator when evidence remains consistent;
- retain Fabio Sobral as sole current maintainer;
- retain previously named contributors as historical contributors;
- remove obsolete contact email from current content and package metadata;
- preserve historical Git author emails;
- reconcile nested `AGENTS.md`, `.agent/PLANS.md`, `NOTICE`, `AUTHORS.md`, and validators with root monorepo policy without deleting useful project-specific instructions;
- update build paths and CI working directories for `vscode-extension/`;
- keep existing commands, Marketplace identity, extension ID, and release versioning unless a path change requires a narrowly documented correction.

Use a commit message equivalent to:

    chore(vscode): normalize imported governance

Run from `vscode-extension/`:

    npm ci
    npm run audit
    npm run compile
    npm test

Also run its existing governance tests during transition, then consolidate duplicated policy only after behavior is proven equivalent. Do not remove a functioning nested validator merely to reduce duplication during the same commit that corrects legal metadata.

Acceptance for this milestone includes a generated report showing every VS Code applicable path, current versus expected year, and no unresolved blanket-year mismatch.

## Milestone 6: Rewrite and import the Gradle plugin history

Create and filter a disposable clone:

    git clone source/totalcross-gradle-plugin.git rewrite/gradle-plugin
    cd rewrite/gradle-plugin
    git checkout main
    git branch backup/pre-filter-main main
    git tag migration-backup-gradle-<short-sha> main
    git filter-repo --force \
      --to-subdirectory-filter gradle-plugin \
      --tag-rename '':'gradle-plugin-'

Validate metadata, topology, tags, path prefix, and repository integrity. Merge it third:

    git remote add import-gradle ../../rewrite/gradle-plugin
    git fetch import-gradle --tags
    git merge --no-ff --allow-unrelated-histories \
      import-gradle/main \
      -m 'chore(import): import Gradle plugin history'

Then create a dedicated Gradle normalization commit. It must:

- preserve Fabio Sobral as original creator if the first-commit and existing attribution evidence agree;
- identify Fabio Sobral as sole current maintainer;
- retain other contributors as historical contributors;
- remove current-tree contact email fields when present;
- apply provenance-based exact years rather than copying `2026` to older files;
- preserve Apache-2.0 as the project license when authoritative;
- reconcile nested governance and validation with the root policy;
- adapt CI and build paths to `gradle-plugin/` without changing plugin IDs, Maven coordinates, or release semantics.

Use a commit message equivalent to:

    chore(gradle): normalize imported governance

Run from `gradle-plugin/`:

    ./gradlew clean test --console=plain
    ./gradlew publishToMavenLocal --console=plain

Use a log file for verbose output and report concise failures. Run network tests only when explicitly configured and document them separately from deterministic tests.

## Milestone 7: Complete monorepo integration without coupling releases

After all three normalized imports pass independently, update root documentation and CI in a separate integration commit.

The root `README.md` must describe:

- purpose of the tooling monorepo;
- directory map;
- per-project build commands;
- independent version and release policy;
- tag naming convention;
- root governance validation command;
- current sole maintainer;
- historical provenance and the limits of the available history evidence;
- links to archived repositories and their issue/release history;
- the fact that issues, PRs, releases, stars, forks, and discussions were not automatically transferred by Git history import.

The root `NOTICE` must contain a clear license migration matrix. At minimum, document:

- Maven plugin original license evidence or the absence of an authoritative license, plus the Apache-2.0 assignment date;
- VS Code historical MIT period and current Apache-2.0 period;
- Gradle plugin Apache-2.0 provenance;
- historical TotalCross copyright through 2021 where applicable;
- Amalgam current-period copyright from the first attributable post-2021 year through 2026;
- preservation of third-party and separately licensed notices;
- distinction between original creators, historical contributors, sole current maintainer, and copyright holders.

Create path-aware CI. A reasonable structure is:

- `governance`: always run root validator and tests;
- `maven-plugin`: run when `maven-plugin/**` or shared governance affecting it changes;
- `vscode-extension`: run when `vscode-extension/**` or shared governance affecting it changes;
- `gradle-plugin`: run when `gradle-plugin/**` or shared governance affecting it changes;
- Do not add a history-audit job to current CI: the checked-out `main` branch has no source-revision inputs for that audit.

Do not introduce a JavaScript monorepo manager, Gradle composite build, Maven reactor, or shared release tool unless it is strictly required to execute the existing projects. Those are future architectural decisions, not part of repository consolidation.

Use a commit message equivalent to:

    ci(tooling): integrate monorepo validation

## Milestone 8: Assess history preservation and contributor attribution

`tools/check-imported-history.py` is a comparison tool, not a standalone validator of the current tree. It reads an external source root containing the three source mirrors, reads the target's source-revision and old-to-new commit mapping data, and compares author/committer metadata, messages, and parent topology for every mapped commit. Its command-line interface requires `--source-root`; invoking it without that argument is expected to fail with usage error 2.

The script remains relevant for a future filtered-history migration, but it is not the correct tool for inventing a map for the current linear reconstruction. The current-tree comparison is complete and its failed exact-preservation result is recorded in `migration/history-audit-report.md`; no claim of exact author/committer/topology preservation is made.

For every mapped commit, compare:

- original and rewritten author name;
- author email;
- author timestamp including timezone;
- committer name;
- committer email;
- committer timestamp including timezone;
- full commit message bytes;
- parent count;
- mapped parent ordering;
- merge status.

Tree IDs and commit IDs are expected to differ because paths changed. Signed commit verification may no longer be valid because rewriting the commit object invalidates signatures. Record the number of signed commits and this limitation; do not claim signatures were preserved.

Compare contributor summaries:

    git shortlog -sne <source-default-branch>
    git shortlog -sne <filtered-default-branch>

Normalize only path-independent presentation, not names or emails. Every original identity tuple must remain represented with the same commit count on the mapped branch, except when a documented source-history anomaly requires explanation.

Validate path history and blame on representative files from each project:

    git log --follow -- maven-plugin/path/to/file
    git log --follow -- vscode-extension/src/extension.ts
    git log --follow -- gradle-plugin/path/to/file

    git blame maven-plugin/path/to/file
    git blame vscode-extension/src/extension.ts
    git blame gradle-plugin/path/to/file

When a future history migration is separately authorized, validate its first-parent import order:

    git log --first-parent --reverse --oneline main

It must show the governance baseline first and import merge commits in this order:

1. Maven plugin;
2. VS Code plugin;
3. Gradle plugin.

Validate tags:

    git tag --list 'maven-plugin-*'
    git tag --list 'vscode-extension-*'
    git tag --list 'gradle-plugin-*'

Acceptance for a future history audit:

- every original default-branch commit maps to exactly one imported commit;
- author and committer metadata match;
- parent relationships match through the map;
- source merge topology is preserved;
- source-revision and old-to-new mapping inputs are complete and parseable;
- representative blame output names original contributors;
- no imported tag collision exists;
- signature invalidation and other unavoidable effects are documented honestly.

## Milestone 9: Validate copyright, exact years, builds, and final behavior

From the monorepo root, run the checks supported by the current `main` tree:

    python3 tools/check-license-headers.py
    python3 -m unittest discover -s tests/license_headers -p 'test_*.py'
    git diff --check
    git status --short --branch

The root validator must:

1. obtain candidate paths from `git ls-files -z`;
2. read the committed provenance manifest;
3. verify each applicable file's exact start year and end year;
4. reject generic years not supported by history;
5. verify the correct holder combination;
6. verify the expected SPDX identifier;
7. verify single-year formatting;
8. preserve shebangs, XML declarations, and required front matter;
9. enforce explicit exclusions;
10. reject obsolete current-tree contact email addresses;
11. verify Fabio Sobral as sole current maintainer and default code owner;
12. verify per-project original creator and historical contributor sections;
13. verify root Apache-2.0 license and NOTICE transition text;
14. print one concise sorted diagnostic per failure;
15. exit zero only when all checks pass.

A successful output should resemble:

    License and provenance validation passed:
      3 imported projects
      <actual applicable-file count> applicable files
      <actual explicit-exclusion count> explicit exclusions
      0 unresolved provenance records
      0 obsolete contact addresses

Use actual counts; do not copy the example numbers.

Then run each project's checks from its directory, using clean dependency state where practical. Capture full output to `evidence/final/` and summarize only relevant success lines or failures.

Create a clean clone from `main` or a local bare remote and repeat the checks there. The independent clone at `/tmp/totalcross-tooling-clean.6pgoDU` passed the root and project checks listed in this plan.

Review the final commit sequence. Governance, each import, each normalization, and final integration must remain logically distinct. Do not squash source history or normalization into one giant commit.

## Milestone 10: Publish move notices in the original repositories

Only after the monorepo is pushed and its default branch is validated, create one narrowly scoped README commit in each original repository.

Place this notice at the top of the Maven README:

    > [!IMPORTANT]
    > This project has moved to the
    > [`TotalCross/totalcross-tooling`](https://github.com/TotalCross/totalcross-tooling)
    > repository under [`maven-plugin/`](https://github.com/TotalCross/totalcross-tooling/tree/main/maven-plugin).
    > Development continues there. This repository is retained for historical
    > issues, pull requests, releases, and links and is intended to be archived.

Use the equivalent `vscode-extension/` and `gradle-plugin/` links in the other repositories.

Do not delete the original README content. Keep historical build and release information below the move notice where useful. Remove obsolete contact email from current README content as part of the same move commit, while retaining creator and historical contributor names.

Use commit messages equivalent to:

    docs: point development to totalcross-tooling

Validate each link anonymously and from the GitHub web UI. Check package registries and Marketplace pages for repository URLs that also need updating. Make those metadata changes in separately reviewable commits or release steps when required.

Archive each original repository only after:

- its move notice is on the default branch;
- the monorepo subdirectory is public and validated;
- current package and Marketplace links no longer direct contributors to the old development location;
- any required final release is published;
- open PRs and issues have been triaged or clearly redirected;
- repository secrets and scheduled workflows have been handled intentionally.

Archiving is a repository setting change and must not be conflated with deleting the repository. Preserve releases, issues, PRs, stars, forks, and historical links.

## Concrete Steps

The following command sequence is representative. Update exact paths and branch names from recorded evidence; do not copy commands blindly if repository state differs.

Create the workspace:

    mkdir -p tooling-migration/{source,rewrite,work,evidence}
    cd tooling-migration/source

Create mirrors and record refs:

    git clone --mirror https://github.com/TotalCross/totalcross-maven-plugin.git totalcross-maven-plugin.git
    git clone --mirror https://github.com/TotalCross/totalcross-vscode-plugin.git totalcross-vscode-plugin.git
    git clone --mirror https://github.com/TotalCross/totalcross-gradle-plugin.git totalcross-gradle-plugin.git
    git clone --mirror https://github.com/TotalCross/totalcross-tooling.git totalcross-tooling.git

    for repo in *.git; do
      git -C "$repo" fsck --full
      git -C "$repo" show-ref --head > "../evidence/${repo%.git}-refs-before.txt"
      git -C "$repo" shortlog -sne --all > "../evidence/${repo%.git}-contributors-before.txt"
      git -C "$repo" rev-list --all --count > "../evidence/${repo%.git}-commit-count-before.txt"
    done

Create the target working clone and governance commit:

    cd ../work
    git clone ../source/totalcross-tooling.git totalcross-tooling
    cd totalcross-tooling
    git switch --orphan main

After creating the required governance files:

    python3 tools/check-license-headers.py
    python3 -m unittest discover -s tests/license_headers -p 'test_*.py'
    git add --all
    git diff --cached --check
    git commit -m 'chore(governance): establish tooling repository baseline'

For each source, perform the filter in a fresh disposable clone, copy the commit map, validate, and merge. Never reuse a failed filtered clone; delete it and recreate it from the mirror.

After all imports and normalizations:

    python3 tools/check-license-headers.py
    python3 -m unittest discover -s tests/license_headers -p 'test_*.py'
    git log --first-parent --reverse --oneline main
    git fsck --full

Do not run `tools/check-imported-history.py` from this checkout without supplying
the external source root and the reviewed mapping inputs it requires. The current
tree does not contain those inputs, so the history audit is an open evidence gap,
not a passing validation step.

Push first to a review branch rather than directly replacing `main` if the remote has any unexpected history:

    git push origin HEAD:refs/heads/migration/consolidate-tooling

Open a draft PR or review the branch locally. Push `main` only after the full validation and explicit publication decision.

Do not force-push the three original repositories. Their only source changes are later README move notices on ordinary branches.

## Validation and Acceptance

The implementation is complete only when all of the following are observable.

### Repository structure

- The root default branch is `main`.
- The current first-parent history is documented accurately; no governance-first import sequence is claimed for `main`.
- Source is located under `maven-plugin/`, `vscode-extension/`, and `gradle-plugin/`.

### Governance

- Root `LICENSE` is canonical Apache License 2.0.
- Root `NOTICE` accurately explains project-specific historical licenses and migration.
- `README.md`, `AUTHORS.md`, `CONTRIBUTING.md`, `AGENTS.md`, and `.agent/PLANS.md` are present and consistent.
- Fabio Sobral is identified as sole current maintainer.
- `.github/CODEOWNERS` contains the default `* @flsobral` rule.
- Original creators are preserved or assigned from the first commit only where explicit attribution was absent.
- Previously listed contributors remain visible as historical contributors.
- No obsolete contact email remains in current first-party content or package metadata.
- Historical Git author emails are unchanged.

### Copyright and license headers

- Every applicable file has an exact provenance record.
- TotalCross start years match each file's actual introduction year.
- The VS Code project contains no unsupported blanket 2020 start-year assumption.
- Files created in 2021 use `2021`, not `2021-2021`.
- Amalgam start years match the file's first attributable post-2021 substantive work, not the repository import date.
- Header-only migration changes do not cause historical files to be falsely classified as Amalgam work.
- Historical original licenses remain represented where applicable.
- Current and mixed-period work uses Apache-2.0 as documented.
- Third-party, generated, and separately licensed files retain authoritative notices.
- Validator tests include regressions for incorrect initial years.

### Git history

- Original authors and history are not claimed to have passed a cross-repository comparison in the absence of the required source and mapping inputs.
- A future history audit must compare author/committer metadata, messages, and parent relationships explicitly.
- Representative `git blame` output shows original contributors.
- Imported tags are namespaced and collision-free.
- Any signature or path-rewrite limitations must be documented only if a future history rewrite is actually performed.

### Builds and tests

- Root governance validator passes.
- Root validator regression tests pass.
- History audit remains pending because its source and mapping inputs are absent from `main`.
- Maven plugin focused tests and package validation pass or have a narrowly evidenced external limitation.
- VS Code `npm ci`, audit, compile, and extension tests pass.
- Gradle plugin tests and local publication validation pass.
- Checks pass from an independent clean clone for the commands recorded in the validation evidence.
- `git diff --check` and `git fsck --full` pass.

### Original repositories

- Each original README begins with a correct move notice and final subdirectory link.
- Existing history, releases, issues, PRs, stars, and forks remain available.
- Current package/Marketplace repository links point to the monorepo where supported.
- The three original repositories are archived only after all prerequisites are verified; GitHub reports them as archived and their historical resources remain available.

## Idempotence and Recovery

Mirror clones under `source/` are immutable backups. Never run `git filter-repo` in them.

Disposable clones under `rewrite/` may be deleted and recreated safely. If a filter or callback is wrong:

    rm -rf rewrite/<project>
    git clone source/<project>.git rewrite/<project>

Do not attempt to repair an uncertain partial rewrite in place.

Before every merge, create a target backup ref:

    git branch backup/before-maven-import
    git branch backup/before-vscode-import
    git branch backup/before-gradle-import

Create each immediately before its corresponding merge. If normalization fails, reset only the disposable migration branch to the recorded backup. Do not alter published refs.

The root validator must not rewrite files. Header application should be a separate explicit command or reviewed edit. Re-running the header application tool must be idempotent: a compliant file must remain byte-for-byte unchanged.

If the target remote unexpectedly receives commits during migration, fetch them, stop publication, record the divergence, and decide whether to rebase the governance/import sequence or merge the external work. Never force-push over unexplained work.

If a source repository receives new commits after the evidence snapshot, either:

1. freeze source development during the final migration window; or
2. rerun the import from fresh mirrors and discard the previous candidate branch.

Do not manually cherry-pick late commits onto a rewritten branch without updating the commit map and validation evidence.

If the original repositories are archived too early, unarchive them, correct the move destination, and only re-archive after validation. Archiving is reversible; deleting releases or repositories is not part of this plan.

## Artifacts and Notes

Retain these artifacts in the repository or migration evidence package:

- `migration/license-provenance.json`;
- `migration/history-audit-report.md`;
- before/after ref inventories;
- before/after commit counts;
- before/after contributor shortlogs;
- signed-commit inventory;
- tag translation report;
- VS Code per-file year correction report;
- validator output;
- build/test summary with full logs stored outside routine CI output;
- clean-clone validation transcript once the pending rerun is completed;
- final first-parent graph;
- links or IDs for migration PRs and original-repository move-notice PRs.

Keep routine logs concise in the ExecPlan. Point to files rather than pasting thousands of build lines.

## Interfaces and Dependencies

The migration tooling should use only stable, inspectable dependencies:

- Git for repository operations;
- `git-filter-repo` for path rewriting and commit maps;
- Python 3 standard library for provenance, validation, and history comparison scripts;
- existing project toolchains for builds and tests;
- GitHub CLI or the GitHub web UI for repository metadata, PRs, links, and archive settings.

Do not add a database or external service for provenance. JSON files committed to the repository are sufficient at this scale.

Define a clear Python model for provenance. An implementation may use dataclasses equivalent to:

    @dataclass(frozen=True)
    class FileProvenance:
        project: str
        source_path: str
        final_path: str
        introduction_commit: str
        introduction_year: int
        first_post_2021_substantive_year: int | None
        original_license: str
        expected_license: str
        expected_copyright_lines: tuple[str, ...]
        excluded: bool
        reason: str

The validator must expose a command-line interface equivalent to:

    python3 tools/check-license-headers.py
    python3 tools/check-license-headers.py --project vscode-extension
    python3 tools/check-license-headers.py --staged

The provenance generator should expose a reviewable mode equivalent to:

    python3 tools/build-license-provenance.py \
      --source ../source-checkouts/totalcross-vscode-plugin \
      --project vscode-extension \
      --output migration/license-provenance.vscode.json

It must never silently overwrite reviewed manual overrides. Generate to a temporary file, compare, and require an explicit update flag.

`tools/check-imported-history.py` currently accepts `--source-root` and an optional
`--project`; it expects the source mirrors and target mapping inputs to exist in
the working tree supplied by the caller. It must produce deterministic output and
nonzero exit status on metadata or topology mismatch when those inputs are
available. It is not a current `main` acceptance check because those inputs are
absent.

## Outcomes & Retrospective

The reviewed `main` branch contains the three project directories, root Apache-2.0 governance, path-aware workflows, and the provenance files. Its first-parent history is a linear reconstruction rather than the governance-first merge sequence described by the original draft, and the plan now records that fact. The execution commits were pushed to `origin/main`.

The current tree contains 11 Maven, 13 VS Code, and 1 Gradle namespaced tags. The current-tree audit located all 157 source commits, but exact history preservation fails for the VS Code and Gradle committer tuples and three project-boundary parent relationships. The supported filtered-history auditor remains conditional on explicit external mapping inputs.

`migration/license-provenance.json` records 16 Maven paths (all applicable), 74 VS Code paths (56 applicable and 18 excluded), and 38 Gradle paths (32 applicable and 6 excluded). The VS Code audit expects 17 applicable 2019 starts and 7 2020 starts; 12 source generic `2020-2021` headers were corrected to 2019, while 7 are retained where file history supports 2020. Twelve VS Code records are mixed-period. Maven's absent historical license is recorded in its NOTICE, VS Code preserves MIT historical provenance, and Gradle retains Apache-2.0 provenance.

The root validator passes and the current regression suite has 17 passing tests. The independent clean clone passed root and VS Code governance, Maven package with tests skipped, Gradle's 18 tests and local publication, and VS Code `npm ci`, audit, compile, and 20 integration tests. Maven's three-test suite still has one reproducible `JavaJDKManagerTest` failure after downloading the remote JDK. The repository-side VS Code 0.0.16 publication is present; the Marketplace item page returned HTTP 200 and displayed 0.0.16 with current monorepo links on 2026-07-16. The three source repositories are archived, and no external Maven/Gradle registry publication configuration was found in the checked-in build descriptors.

## Editorial Report

### Editorial Summary

The current `main` tree contains three independently buildable TotalCross tooling projects under their final subdirectories, with shared Apache-2.0 governance and per-file provenance validation. A read-only lineage report now records which source commits are present and which exact-history properties cannot be claimed without rewriting `main`.

### Original Plan versus Actual Outcome

The requested directory layout, independent project builds, governance policy, provenance manifest, VS Code year report, and local validation are present. The original plan's candidate-branch commit sequence was removed from the current record because it is not part of `main`; the current lineage audit documents the resulting metadata/topology limitations. The repository-side VS Code 0.0.16 publication is complete and all three source repositories are archived after their move notices and open-work redirects were verified.

### What Changed

The core artifacts are `migration/license-provenance.json`, `migration/history-audit-report.md`, `migration/vscode-year-report.md`, `tools/check-license-headers.py`, `tools/check-imported-history.py`, and the root workflows. The three imported projects are `maven-plugin/`, `vscode-extension/`, and `gradle-plugin/`.

### Decisions and Trade-offs

Per-file provenance costs an actively maintained manifest but avoids fictitious repository-wide copyright years. Independent builds avoid a premature monorepo build system. The history auditor is retained as a conditional diagnostic tool, but its output is excluded until source mirrors and mapping inputs are available.

### Unexpected Problems and Discoveries

The evidence includes the unsafe VS Code 2020 blanket year, Maven's absent source license and macOS JDK test failure, the current-main history mismatches, and the successful independent clean clone. The VS Code path-length workaround was not imported because the failure was not reproduced.

### Validation and Measurable Results

Observed results are: 128 provenance records (16 Maven, 74 VS Code, 38 Gradle); root license validation passes; 17 root regression tests pass; 18 Gradle tests have zero failures/errors and local publication succeeds; 20 VS Code integration tests pass in the working tree and clean clone; Maven package succeeds but 1 of 3 Maven tests fails reproducibly for its remote JDK layout expectation; and the Marketplace item currently exposes VS Code 0.0.16 with monorepo links.

### Useful Evidence and Examples

Use `migration/history-audit-report.md`, `migration/vscode-year-report.md`, the current first-parent graph, the clean-clone commands in this plan, the four Gradle XML reports under `gradle-plugin/build/test-results/test/`, and [the current Marketplace item](https://marketplace.visualstudio.com/items?itemName=TotalCross.vscode-totalcross) as concise evidence.

### Limitations, Remaining Work, and Open Questions

This monorepo does not migrate issues, pull requests, releases, stars, forks, discussions, or secrets. It does not unify releases or build systems. The Maven JDK test needs a separately scoped portability fix. Exact source committer/topology preservation cannot be claimed without rewriting `main`, which remains prohibited. Maven and Gradle local publication is validated, but no external registry publication is configured in the checked-in descriptors. Older Marketplace versions remain in version history even though 0.0.16 has current monorepo links.

### Possible Article Angles

Useful angles are preserving Git identity through multi-repository path rewrites, using file history instead of repository age for copyright years, and designing prospective licensing governance for a polyglot monorepo.

### Suggested Narrative

Start with the need to consolidate tools without coupling their builds; show the VS Code per-file year discovery; explain the current-main lineage audit and why exact history preservation cannot be repaired safely; then show clean-clone validation, publication status, and remaining limitations.

### Claims Requiring Human Review

The Maven prospective Apache assignment, copyright-holder descriptions, creator lists, historical contributor completeness, external repository archival, and Marketplace publication claims require maintainer and legal/editorial review.

## Revision note

Initial version created on 2026-07-16. It proposed consolidating the Maven, VS Code, and Gradle plugin repositories into `TotalCross/totalcross-tooling`, establishing Apache-2.0 governance, preserving original commit contributors through path rewrites, normalizing attribution and contacts, validating exact per-file copyright years, correcting the VS Code project's unsafe generic 2020 start-year rule, and redirecting the original repositories for archival.

Execution revision on 2026-07-16: clarified and committed the current-main history audit, made the history checker accept explicit inputs, delegated VS Code governance validation to the root provenance policy, completed working-tree and independent clean-clone validation, recorded the Maven portability failure, confirmed VS Code 0.0.16 Marketplace metadata, pushed the execution commits to `origin/main`, and archived the three source repositories after verifying their redirects and backups. Exact history repair remains prohibited; no external Maven/Gradle registry publication is configured.
