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

The migration must preserve the original contributors and commit identities. Moving files into subdirectories necessarily changes commit object IDs when `git filter-repo` rewrites each tree, but it must not replace original authors, committers, author dates, committer dates, messages, or merge relationships. The old-to-new commit mapping must be retained and validated.

After completion, a developer must be able to:

1. browse each imported project's complete rewritten history under its final subdirectory;
2. run `git blame` and see the original contributors rather than a migration account;
3. see Fabio Sobral (`@flsobral`) identified as the sole current maintainer and default code owner;
4. understand the original creator and historical contributors of each project without obsolete contact email addresses;
5. run one root governance command that verifies license headers, provenance years, project attribution, exclusions, and repository-level policy;
6. build and test each plugin with its existing toolchain from its new directory;
7. follow links from the archived source repositories to the corresponding location in `totalcross-tooling`.

## Preliminary verified context

The following facts were verified while preparing this plan and must be rechecked against the actual clones before mutation:

- `TotalCross/totalcross-tooling` exists, is empty, and uses `main` as its default branch.
- `totalcross-maven-plugin` uses `master` as its default branch.
- `totalcross-vscode-plugin` uses `master` as its default branch.
- `totalcross-gradle-plugin` uses `main` as its default branch.
- The Maven plugin currently has no top-level `LICENSE` file in its default branch.
- The VS Code project currently has an Apache-2.0 root license and a `NOTICE` describing an earlier MIT period.
- The VS Code project's current governance identifies Italo Yeltsin (`@ItaloYeltsin`) as original creator and Fabio Sobral (`@flsobral`) as sole current maintainer.
- The Gradle plugin currently uses Apache-2.0 and identifies Fabio Sobral as creator and maintainer.
- The VS Code initial commit is `d5a1acd1eeb9e04d9d1082dc1e6ca3db0367c432`, dated 2019-11-19, authored by Italo Yeltsin, and its original MIT license says `Copyright (c) 2019 TotalCross Global Mobile Platform`.
- The current VS Code header policy includes historical headers such as `Copyright (C) 2020-2021 TotalCross Global Mobile Platform Ltda.`. That blanket start year is not sufficient evidence for all files because the repository began in 2019. It may be correct for a particular file created in 2020, but it is wrong for any applicable file introduced in 2019.

The last point is a required correction, not merely a possible follow-up. The migration must calculate the initial year independently for every applicable file. Do not change all VS Code files to 2019, and do not retain all of them as 2020. Use each file's own history.

## Progress

Use UTC timestamps on every completed entry. Split partially completed entries into explicit completed and remaining portions at every stopping point.

- [x] (2026-07-16 15:20Z) Added `.agent/PLANS.md`, this living ExecPlan, root governance files, CODEOWNERS, governance workflow, and root validator.
- [x] (2026-07-16 15:20Z) Created immutable local source mirrors, recorded refs and contributor inventories, and rewrote disposable clones into `maven-plugin/`, `vscode-extension/`, and `gradle-plugin/`.
- [x] (2026-07-16 15:20Z) Merged the three default-branch histories in Maven, VS Code extension, Gradle order; committed namespaced tags and old-to-new commit maps.
- [x] (2026-07-16 15:20Z) Added a deterministic 128-record import provenance inventory and offline commit-map audit.
- [x] (2026-07-16 16:10Z) Rebuilt the migration from source mirrors after a snapshot-only branch lost reachable imported history; added path-aware cloud CI for Maven, VS Code extension, and Gradle validation, and verified the rebuilt commit maps from a clean clone.
- [ ] Run clean-clone validation, complete source-repository move notices, and archive the source repositories only after publication approval.

- [ ] Record the exact source and target branch heads, repository visibility, tags, branches, releases, open pull requests, and working-tree state before mutation.
- [ ] Create local mirror backups and immutable backup refs for all four repositories.
- [ ] Inventory existing licenses, notices, attribution, contacts, code owners, agent instructions, CI, build entry points, generated files, vendored code, and release automation in all three source repositories.
- [ ] Generate a per-file provenance inventory for each source repository before any path rewrite or header-only migration edit.
- [ ] Validate and correct the VS Code creation-year rule so every applicable file starts at its actual introduction year rather than the generic year 2020.
- [ ] Determine the original creator of each project from explicit project attribution or, when absent, from the author of the first commit.
- [ ] Determine historical contributors from repository documentation and Git history while distinguishing them from current maintainers.
- [ ] Determine the authoritative original license of each source repository and document any ambiguity.
- [ ] Create the first governance-only commit in `totalcross-tooling` with README, Apache-2.0 LICENSE, NOTICE, AUTHORS, CONTRIBUTING, AGENTS, `.agent/PLANS.md`, CODEOWNERS, validator, validator tests, and governance CI.
- [ ] Validate the governance-only commit independently before importing any source history.
- [ ] Rewrite a disposable Maven plugin clone into `maven-plugin/`, preserve commit metadata and topology, namespace tags, and save its old-to-new commit map.
- [ ] Merge the Maven history into `totalcross-tooling` as the first imported history using a merge whose first parent is the governance branch.
- [ ] Normalize Maven plugin attribution, contacts, copyright headers, license metadata, NOTICE information, and build validation in a dedicated post-import commit.
- [ ] Rewrite a disposable VS Code plugin clone into `vscode-extension/`, preserve commit metadata and topology, namespace tags, and save its old-to-new commit map.
- [ ] Merge the VS Code history as the second imported history using a merge whose first parent is the current monorepo branch.
- [ ] Normalize VS Code attribution and headers in a dedicated post-import commit, including exact per-file initial-year corrections.
- [ ] Rewrite a disposable Gradle plugin clone into `gradle-plugin/`, preserve commit metadata and topology, namespace tags, and save its old-to-new commit map.
- [ ] Merge the Gradle history as the third imported history using a merge whose first parent is the current monorepo branch.
- [ ] Normalize Gradle plugin attribution, contacts, copyright headers, license metadata, NOTICE information, and build validation in a dedicated post-import commit.
- [ ] Add monorepo-level CI orchestration and path-aware validation without forcing the three projects into one build system or one shared version.
- [ ] Validate authors, committers, dates, messages, merge topology, tags, blame lineage, commit counts, and old-to-new mappings for all imported histories.
- [ ] Run the root governance validator and each project's focused build and test suite from a clean clone of the candidate monorepo.
- [ ] Update the README of each original repository with a move notice pointing to its new subdirectory.
- [ ] Validate links, releases, package metadata, marketplace metadata, Maven/Gradle publication metadata, and automation references before archiving.
- [ ] Archive the original repositories only after the new repository and move notices are published and independently verified.
- [ ] Finalize `Outcomes & Retrospective` and the evidence-based `Editorial Report`.

## Surprises & Discoveries

Record repository-specific findings here as implementation proceeds. Do not silently resolve licensing, authorship, or historical ambiguity.

- Observation: The VS Code repository began in 2019, but its current historical source-header convention uses a generic `2020-2021` range.
  Evidence: Initial commit `d5a1acd1eeb9e04d9d1082dc1e6ca3db0367c432` is dated 2019-11-19 and contains the original 2019 MIT copyright notice. Current files such as `src/extension.ts` use `2020-2021`. This proves that a repository-wide hard-coded start year is unsafe; it does not by itself prove the correct year for any individual source file.

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

- Decision: Make the governance baseline the first commit on the target branch's first-parent history.
  Rationale: Governance, licensing policy, validation, code ownership, and agent instructions must exist before imported code is normalized or extended.
  Date/Author: 2026-07-16 / Fabio Sobral requirement

- Decision: Import each rewritten source history with a non-fast-forward merge whose first parent is the current `totalcross-tooling/main` commit.
  Rationale: `git log --first-parent main` will show governance first, followed by Maven, VS Code, and Gradle imports in the requested order, while each imported history remains reachable as the merge's second parent.
  Date/Author: 2026-07-16 / OpenAI

- Decision: Use `git filter-repo --to-subdirectory-filter` in disposable clones.
  Rationale: Every historical checkout then shows files under the final subdirectory, and ordinary `git log`, `git blame`, and path history are more natural than a subtree merge whose old commits keep files at repository root.
  Date/Author: 2026-07-16 / OpenAI

- Decision: Preserve original Git identity metadata and do not replace author or committer fields with Fabio Sobral or the migration operator.
  Rationale: Current maintenance is repository governance, not retroactive authorship. Contributor recognition depends on preserving original names, emails, and dates.
  Date/Author: 2026-07-16 / Fabio Sobral requirement

- Decision: Removing contact email applies to the current working tree and current metadata, not historical commit author emails.
  Rationale: Rewriting author emails would damage contributor attribution and is not equivalent to removing an obsolete public contact address. Historical Git identity data remains intact unless a separate privacy/legal instruction explicitly requires rewriting it.
  Date/Author: 2026-07-16 / OpenAI

- Decision: Namespace imported tags as `maven-plugin-*`, `vscode-extension-*`, and `gradle-plugin-*`.
  Rationale: Common tags such as `v1.0.0` can collide in a monorepo. Namespaced tags remain understandable and independently releasable.
  Date/Author: 2026-07-16 / OpenAI

- Decision: Store old-to-new commit maps generated by `git filter-repo` under `migration/commit-maps/` in the monorepo.
  Rationale: Rewritten hashes must remain auditable. The map lets maintainers translate old issue, PR, and documentation references to the imported history.
  Date/Author: 2026-07-16 / OpenAI

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

Copy `.git/filter-repo/commit-map` to the target as:

    migration/commit-maps/maven-plugin.txt

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

    git clone source/totalcross-vscode-plugin.git rewrite/vscode-plugin
    cd rewrite/vscode-plugin
    git checkout master
    git branch backup/pre-filter-master master
    git tag migration-backup-vscode-<short-sha> master
    git filter-repo --force \
      --to-subdirectory-filter vscode-plugin \
      --tag-rename '':'vscode-plugin-'

Save the commit map as:

    migration/commit-maps/vscode-plugin.txt

Run the same metadata, topology, tag, path, and `git fsck` validations as for Maven.

Merge it second:

    git remote add import-vscode ../../rewrite/vscode-plugin
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

Save the commit map as:

    migration/commit-maps/gradle-plugin.txt

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
- historical provenance and commit-map location;
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
- `history-audit`: run the committed provenance and commit-map consistency checks when migration metadata changes.

Do not introduce a JavaScript monorepo manager, Gradle composite build, Maven reactor, or shared release tool unless it is strictly required to execute the existing projects. Those are future architectural decisions, not part of repository consolidation.

Use a commit message equivalent to:

    ci(tooling): integrate monorepo validation

## Milestone 8: Validate history preservation and contributor attribution

Create a deterministic audit script, such as `tools/check-imported-history.py`, that compares each original mirror with its filtered branch using the saved commit map.

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

Validate first-parent import order:

    git log --first-parent --reverse --oneline main

It must show the governance baseline first and import merge commits in this order:

1. Maven plugin;
2. VS Code plugin;
3. Gradle plugin.

Validate tags:

    git tag --list 'maven-plugin-*'
    git tag --list 'vscode-extension-*'
    git tag --list 'gradle-plugin-*'

Acceptance for this milestone:

- every original default-branch commit maps to exactly one imported commit;
- author and committer metadata match;
- parent relationships match through the map;
- source merge topology is preserved;
- commit maps are complete and parseable;
- representative blame output names original contributors;
- no imported tag collision exists;
- signature invalidation and other unavoidable effects are documented honestly.

## Milestone 9: Validate copyright, exact years, builds, and final behavior

From the monorepo root, run:

    python3 tools/check-license-headers.py
    python3 -m unittest discover -s tests/license_headers -p 'test_*.py'
    python3 tools/check-imported-history.py
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
      487 applicable files
      31 explicit exclusions
      0 unresolved provenance records
      0 obsolete contact addresses

Use actual counts; do not copy the example numbers.

Then run each project's checks from its directory, using clean dependency state where practical. Capture full output to `evidence/final/` and summarize only relevant success lines or failures.

Create a clean clone from the candidate target repository or local bare remote and repeat the checks there. This proves that no untracked files or local-only dependencies are masking the result.

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
    python3 tools/check-imported-history.py
    git log --first-parent --reverse --oneline main
    git fsck --full

Push first to a review branch rather than directly replacing `main` if the remote has any unexpected history:

    git push origin HEAD:refs/heads/migration/consolidate-tooling

Open a draft PR or review the branch locally. Push `main` only after the full validation and explicit publication decision.

Do not force-push the three original repositories. Their only source changes are later README move notices on ordinary branches.

## Validation and Acceptance

The implementation is complete only when all of the following are observable.

### Repository structure

- The root default branch is `main`.
- The first-parent history begins with the governance baseline.
- The three import merge commits appear in Maven, VS Code, Gradle order.
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

- Original authors, author emails, author dates, committers, committer emails, committer dates, and messages match through the commit maps.
- Merge parent relationships are preserved through the maps.
- Representative `git blame` output shows original contributors.
- Commit maps are committed under `migration/commit-maps/`.
- Imported tags are namespaced and collision-free.
- Signature invalidation caused by rewriting is documented.
- No source history is squashed.

### Builds and tests

- Root governance validator passes.
- Root validator regression tests pass.
- History audit passes.
- Maven plugin focused tests and package validation pass or have a narrowly evidenced external limitation.
- VS Code `npm ci`, audit, compile, and extension tests pass.
- Gradle plugin tests and local publication validation pass.
- Checks pass from a clean clone.
- `git diff --check` and `git fsck --full` pass.

### Original repositories

- Each original README begins with a correct move notice and final subdirectory link.
- Existing history, releases, issues, PRs, stars, and forks remain available.
- Current package/Marketplace repository links point to the monorepo where supported.
- Repositories are archived only after all prerequisites are verified.

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
- `migration/commit-maps/maven-plugin.txt`;
- `migration/commit-maps/vscode-plugin.txt`;
- `migration/commit-maps/gradle-plugin.txt`;
- before/after ref inventories;
- before/after commit counts;
- before/after contributor shortlogs;
- signed-commit inventory;
- tag translation report;
- VS Code per-file year correction report;
- validator output;
- build/test summary with full logs stored outside routine CI output;
- clean-clone validation transcript;
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

The history auditor must accept source mirrors and commit maps through explicit arguments or a checked-in config. It must produce deterministic output and nonzero exit status on metadata or topology mismatch.

## Outcomes & Retrospective

Complete this section during execution and at every major milestone. At completion, summarize:

- final commit sequence and merge IDs;
- number of commits imported from each source;
- number of authors and committers preserved;
- number of tags imported and renamed;
- number of files inspected, normalized, mixed-period, and excluded;
- exact VS Code year corrections, including how many `2020` starts changed to `2019` or another year and how many remained `2020` because history supported them;
- original creator and historical contributor decisions for each project;
- license evidence and transition for each project;
- obsolete contact occurrences removed from current content;
- validation commands and observed outcomes;
- build/test results;
- signature, release, issue, PR, fork, and archive limitations;
- original-repository move notice and archive status;
- remaining follow-up work.

Do not mark the plan complete while this section contains only placeholders.

## Editorial Report

This section is mandatory at completion. Keep it factual, evidence-based, and synchronized with the final repository state.

### Editorial Summary

Describe why consolidating three tooling repositories mattered, what developers can now find and validate in one place, and how contributor history remained visible.

### Original Plan versus Actual Outcome

State which repositories and refs were imported, whether the requested order and directory names were preserved, and any changes made because of licensing, build, or history discoveries.

### What Changed

Name the final paths, governance tools, workflows, provenance files, commit maps, tag conventions, and move notices.

### Decisions and Trade-offs

Explain why a new repository, `git filter-repo`, independent releases, first-parent import ordering, prospective Apache governance, and per-file year calculation were chosen. Describe the costs: changed commit hashes, invalidated signatures, retained original issue/release repositories, and added provenance maintenance.

### Unexpected Problems and Discoveries

Include the VS Code generic-year mismatch and any other license, attribution, build-root, tag, or metadata surprises with concise evidence.

### Validation and Measurable Results

Record only observed commands and actual counts. Include clean-clone builds, validator test totals, imported commit counts, contributor identity comparisons, and exact year-correction counts.

### Useful Evidence and Examples

Point to representative commit maps, provenance records, validator regression tests, blame results, merge graph, move-notice commits, and concise build transcripts.

### Limitations, Remaining Work, and Open Questions

State what the monorepo does not yet unify, including shared tooling-core extraction, common release automation, issue migration, and any unresolved licensing authority requiring human review.

### Possible Article Angles

Suggest technically honest angles such as:

- preserving contributor identity while moving multiple repositories into subdirectories;
- validating copyright years from file history instead of repository age;
- designing license governance for a polyglot monorepo with historical license periods;
- using first-parent history to make a multi-repository import understandable.

### Suggested Narrative

Outline the strongest article as problem, constraints, alternatives, governance-first baseline, history rewrite, per-file provenance discovery, import sequence, validation evidence, limitations, and next steps.

### Claims Requiring Human Review

List any claims about relicensing authority, copyright ownership, creator attribution, historical contributor completeness, or absent-license treatment that require legal or maintainer review. Even when no special claim remains, require normal technical and editorial review.

## Execution update: supplied Maven downloader files

On 2026-07-16, the supplied `JavaJDKManager.java` and `JavaJDKManagerTest.java` versions replaced the local versions. The test now uses a fresh temporary directory and the macOS JDK home is resolved as `Contents/Home` under the extracted bundle. The focused URL test passes. The complete Maven suite was retried and remains blocked in this environment because the Java process cannot reach the configured network proxy; the SDK tests additionally expose an AWS SDK proxy-handling null dereference.

## Revision note

Initial version created on 2026-07-16. It consolidates the Maven, VS Code, and Gradle plugin repositories into `TotalCross/totalcross-tooling`, establishes Apache-2.0 governance first, preserves original commit contributors through mapped path rewrites, normalizes attribution and contacts, validates exact per-file copyright years, corrects the VS Code project's unsafe generic 2020 start-year rule, and redirects the original repositories for archival.

## Execution update

The local migration baseline and three rewritten histories were completed on 2026-07-16. The remaining publication actions require access to the upstream repositories and explicit publication approval. A warm local Maven cache allowed one focused run to complete, but a clean clone still cannot reach the legacy Azul and TotalCross S3 download endpoints from this execution environment. Cloud CI now runs the Maven test where the newly enabled external-download access is available. The migration was rebuilt from source mirrors because the snapshot-only branch did not retain the rewritten imported commits required by the committed maps.

## Execution update: Azul metadata endpoint

On 2026-07-16, the Maven downloader was aligned with `gradle-plugin/src/main/java/com/totalcross/gradle/JdkResolver.java`: both use Azul’s `bundles/latest/binary` endpoint with `crac_supported=false`. A focused regression test verifies the generated URL. The local Java process still cannot resolve the environment proxy, so the real download remains a cloud-CI validation concern.

## Revision note

Updated on 2026-07-16 to name the final VS Code subproject `vscode-extension/`, as requested. References to the historical source repository `totalcross-vscode-plugin` intentionally remain unchanged.
