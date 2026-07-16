<!--
Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->

# Bootstrap Apache-2.0 licensing, authorship, repository governance, and contextual project history

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This plan must be maintained in accordance with the repository's `.agent/PLANS.md`. Because this is a fresh repository, the first commit created by this plan must add `.agent/PLANS.md` together with the repository's licensing, attribution, validation, README, and agent-governance files before any project implementation is committed.

## Purpose / Big Picture

The repository is new and does not require any Git history rewriting. The objective is to establish its legal, attribution, contribution, validation, and agent-operating foundations in a dedicated first commit, then import or commit the project implementation in a reviewable history that reflects the current context of the files whenever that context can be determined reliably.

After this change:

1. the repository is licensed under Apache License 2.0;
2. applicable first-party source files use standardized SPDX headers;
3. the copyright holder is written exactly as:

       SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.

4. the license identifier is written exactly as:

       SPDX-License-Identifier: Apache-2.0

5. Fabio Sobral is visibly identified as the project creator and lead maintainer without being presented as the legal copyright holder;
6. `LICENSE`, `NOTICE` when appropriate, `AUTHORS.md`, `README.md`, `CONTRIBUTING.md`, `.github/CODEOWNERS`, the validation tooling and CI, `AGENTS.md`, and `.agent/PLANS.md` are committed separately from the project implementation in the first commit;
7. contributors and coding agents can run one documented validation command and receive a clear success or failure result;
8. the remaining project files are committed in coherent, contextual commits when their purpose can be inferred from the repository state, dependencies, filenames, build structure, and implementation relationships;
9. when a trustworthy contextual split is not possible, the remaining project is committed in one implementation commit rather than inventing history or misleading rationale.

The implementation must distinguish legal ownership from project attribution:

- `Amalgam Solucoes em TI Ltda.` is the copyright holder.
- `Fabio Sobral` is the creator and lead maintainer.
- Fabio Sobral must not be added as a personal copyright holder unless a separate legal decision explicitly requires it.

A contributor must be able to clone the repository, inspect the first commit to understand its legal and operational baseline, run the documented validation command, and build or test the project using the documented repository entry points.

## Progress

- [x] (2026-07-15 18:00Z) Inspected the working tree, Git state, remote, build entry points, instructions, and this ExecPlan.
- [x] (2026-07-15 18:00Z) Confirmed that `main` has zero commits and no local or remote refs; no history needs preservation or rewriting.
- [x] (2026-07-15 18:00Z) Inventoried the tree: Gradle plugin sources and tests are first-party implementation; `examples/basic-app` is a first-party example; `gradle/wrapper/gradle-wrapper.jar` and `gradlew` are Gradle-generated third-party material; `build/` and `.gradle/` are generated output and ignored.
- [x] (2026-07-15 18:00Z) Confirmed Apache License 2.0 is the intended first-party license. The Gradle wrapper retains its own Apache notice and is excluded from first-party header validation.
- [x] (2026-07-15 18:10Z) Defined canonical SPDX-FileCopyrightText and Apache-2.0 headers for C-style, hash-comment, Markdown, and XML files; wrapper and legal texts are explicitly excluded.
- [x] (2026-07-15 18:10Z) Created the complete repository foundation: Apache license and notice, attribution, contribution rules, root agent guidance, planning policy, CODEOWNERS, CI, validator, and validator tests.
- [x] (2026-07-15 18:10Z) Implemented the dependency-free `tools/check-license-headers.py` validator and its focused regression tests.
- [x] (2026-07-15 18:10Z) Added `.github/workflows/license-headers.yml`, which runs the documented validator and its tests for pushes and pull requests.
- [x] (2026-07-15 18:10Z) Updated `README.md`, `AUTHORS.md`, `CONTRIBUTING.md`, `.github/CODEOWNERS`, `AGENTS.md`, and `.agent/PLANS.md`.
- [x] (2026-07-15 18:11Z) Staged and reviewed only foundation files, then created `fd4a254 chore(repo): establish licensing and governance` without project implementation files.
- [x] (2026-07-15 18:15Z) Added canonical SPDX headers to all applicable first-party build scripts, Java sources, tests, examples, and planning documents before committing them.
- [x] (2026-07-15 18:15Z) Identified three evidence-based groups: cohesive plugin implementation, a composite-build example, and a focused correction to the example's legacy deployment configuration.
- [x] (2026-07-15 18:17Z) Committed `bfc3b12 feat: add TotalCross Gradle plugin implementation`, `7c2f13d docs(example): add basic TotalCross application`, and `1d513e0 fix(example): use the legacy Linux deployment path`.
- [x] (2026-07-15 18:20Z) Ran license validation, validator tests, Gradle test/check, Maven Local publication, the opt-in SDK network test, and the example package task.
- [x] (2026-07-15 18:20Z) Reviewed the commit sequence and working tree; no implementation files were omitted or assigned speculative historical meaning.
- [x] (2026-07-15 18:20Z) Recorded final results and the Editorial Report.

## Surprises & Discoveries

Record repository-specific findings here as implementation proceeds.

- Observation: The repository has no commits and `origin/main` is marked gone locally; `git rev-list --all --count` returned `0`.
  Evidence: `git status --short --branch` reported `## No commits yet on main...origin/main [gone]`, and `git show-ref --heads --tags` produced no refs.

- Observation: Existing first-party files use an earlier `Copyright (C)` header, while this ExecPlan requires `SPDX-FileCopyrightText`.
  Evidence: `rg` found the earlier form in Java, Gradle, Markdown, and ignore files; the required exact form is stated in this plan.

- Observation: The repository convention and user instruction require planning files under `.agent/`.
  Evidence: `.agent/PLANS.md` exists, `AGENTS.md` refers to it, and the user corrected the path while this plan was being executed.

- Observation: The basic example initially targeted multiple platforms, while its minimal documented use case is Linux and SDK 7.2.2 requires the legacy Java 8-to-7 path.
  Evidence: After selecting Linux only, `./gradlew -p examples/basic-app clean totalcrossPackage --console=plain` completed and produced Linux deployment artifacts.

Examples include:

- the repository is not actually empty or contains a published commit that should not be replaced;
- a remote branch already has collaborators or consumers;
- project files contain third-party notices or incompatible license terms;
- generated files are mixed with hand-maintained source;
- a build system or source generator already emits headers;
- filenames suggest one context but dependencies or code show another;
- some files cannot be assigned to a truthful contextual commit without reconstructing history from external evidence;
- a mandatory first-line construct such as a shebang, XML declaration, encoding declaration, or generated marker affects header placement.

Do not silently normalize exceptions. Document each material exception and its resolution.

## Decision Log

- Decision: Do not rewrite Git history.
  Rationale: This is a fresh repository. A clean forward-only commit sequence provides the required structure without changing commit identities or fabricating earlier history.
  Date: 2026-07-15

- Decision: Use the exact license identifier `Apache-2.0`.
  Rationale: This project is explicitly licensed under Apache License 2.0, and SPDX defines `Apache-2.0` as its canonical identifier.
  Date: 2026-07-15

- Decision: Use the exact copyright line:

      SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.

  Rationale: This is the required legal copyright holder and year for first-party project files.
  Date: 2026-07-15

- Decision: Put all repository legal and operational foundations in the first commit, separate from project implementation.
  Rationale: The repository should have a clear baseline establishing license, attribution, contributor rules, validation, CI, README, agent instructions, and planning conventions before implementation appears.
  Date: 2026-07-15

- Decision: The first commit must include, as applicable, `LICENSE`, `NOTICE`, `README.md`, `AUTHORS.md`, `CONTRIBUTING.md`, `.github/CODEOWNERS`, the license validator, validator tests or fixtures, the CI workflow that runs it, `AGENTS.md`, and `.agent/PLANS.md`.
  Rationale: These files form one coherent repository-foundation change. Keeping them together while excluding implementation makes the legal and operating baseline independently reviewable.
  Date: 2026-07-15

- Decision: Identify Fabio Sobral as `Creator and Lead Maintainer` in repository-level documentation.
  Rationale: Project authorship and maintenance should be visible without changing or confusing legal ownership.
  Date: 2026-07-15

- Decision: Do not add personal `@author` tags or maintainer comments throughout source files.
  Rationale: Repository metadata and Git history provide more maintainable attribution and avoid confusing authorship with copyright ownership.
  Date: 2026-07-15

- Decision: Commit remaining project files by their current functional context only when that context is supported by evidence in the working tree.
  Rationale: Coherent commits improve reviewability, but invented chronology or speculative reasons would produce misleading history.
  Date: 2026-07-15

- Decision: Fall back to one project implementation commit when a trustworthy split is not possible.
  Rationale: A truthful broad commit is preferable to multiple artificial commits that falsely imply an implementation sequence.
  Date: 2026-07-15

- Decision: Do not modify third-party copyright or license notices.
  Rationale: Vendored, imported, generated, subtree-managed, or separately licensed files retain their authoritative legal metadata.
  Date: 2026-07-15

- Decision: Keep `.agent/PLANS.md` as the canonical planning policy and update this ExecPlan to use the same path everywhere.
  Rationale: The user explicitly confirmed `.agent/` is the correct repository path. A single canonical location avoids duplicate planning policies and conflicting instructions.
  Date/Author: 2026-07-15 / Codex

- Decision: Replace legacy first-party `Copyright (C)` headers with the exact SPDX-FileCopyrightText form required by this ExecPlan.
  Rationale: A deterministic validator must have one canonical ownership marker. The existing marker is superseded before the implementation commit; Gradle-generated wrapper files are excluded instead of being relicensed.
  Date/Author: 2026-07-15 / Codex

- Decision: Validation must be deterministic and runnable locally as well as in CI.
  Rationale: Contributors and automated agents should detect licensing mistakes before changes are submitted.
  Date: 2026-07-15

- Decision: Commit the build, Gradle wrapper, plugin source, and tests together as `bfc3b12`.
  Rationale: The `java-gradle-plugin` configuration names the plugin implementation classes and tests exercise their public behavior, so separating them would leave a knowingly incomplete build.
  Date/Author: 2026-07-15 / Codex

- Decision: Keep the basic example Linux-only and use the normal SDK 7.2.2 compatibility path.
  Rationale: This is the smallest reproducible example and avoids hard-coded machine paths or optional mobile platform tooling. It demonstrably packages through Retrolambda and JDK 11.
  Date/Author: 2026-07-15 / Codex

## Outcomes & Retrospective

The repository was confirmed fresh: before bootstrap, `main` had zero commits,
no refs, and `origin/main` was locally marked gone. No history was rewritten.

The foundation commit is `fd4a254 chore(repo): establish licensing and
governance`. Its 13 files are `.agent/PLANS.md`, this ExecPlan,
`.github/CODEOWNERS`, `.github/workflows/license-headers.yml`, `.gitignore`,
`AGENTS.md`, `AUTHORS.md`, `CONTRIBUTING.md`, `LICENSE`, `NOTICE`, `README.md`,
`tests/license_headers/test_check_license_headers.py`, and
`tools/check-license-headers.py`. It contains no plugin code, tests, examples,
or Gradle wrapper files.

The final validator inspected 32 applicable first-party files. `LICENSE`,
`NOTICE`, `.github/CODEOWNERS`, `gradlew`, and `gradle/wrapper/` were excluded
because they are legal text, a non-comment metadata file, or Gradle-generated
third-party material. No incompatible first-party or vendored license was
found. The local command is `python3 tools/check-license-headers.py`; CI runs
it and `python3 tests/license_headers/test_check_license_headers.py` in the
`license-headers` job.

The contextual commits are `bfc3b12 feat: add TotalCross Gradle plugin
implementation` for the complete Gradle build, wrapper, Java implementation,
and associated tests; `7c2f13d docs(example): add basic TotalCross application`
for the composite-build example; and `1d513e0 fix(example): use the legacy
Linux deployment path` for the minimal compatibility correction. This is not
the fallback single implementation strategy: the build/source/test group and
example group had independent, observable purposes. The final documentation
commit containing this completed report follows these commits.

Observed successful commands were `python3 tools/check-license-headers.py`,
`python3 tests/license_headers/test_check_license_headers.py`, `./gradlew test
--console=plain`, `./gradlew check --console=plain`, `./gradlew
publishToMavenLocal --console=plain`, `./gradlew sdkSourceNetworkTest
--console=plain`, and `./gradlew -p examples/basic-app clean
totalcrossPackage --console=plain`. The example produced the Linux executable,
`MainWindow.tcz`, `TCBase.tcz`, `TCUI.tcz`, and `libtcvm.so` in
`examples/basic-app/build/totalcross/install/linux/`.

## Editorial Report

### Editorial Summary

The repository began with a working but uncommitted Gradle plugin and no legal
or operational baseline. The completed work establishes a reviewable Apache
2.0 foundation first, then records the plugin and its runnable example in
functional commits. Contributors can now validate SPDX metadata locally and in
CI before using the documented Gradle build.

### Original Plan versus Actual Outcome

The plan called for a legal foundation commit followed by contextual project
commits. That result was delivered. The plan's initial plural planning-directory
spelling was corrected to the repository's user-confirmed `.agent/` path before
the first commit. The fallback single implementation commit was not used because
the example had a separately testable purpose.

### What Changed

`LICENSE`, `NOTICE`, `AUTHORS.md`, `CONTRIBUTING.md`, `AGENTS.md`, and
`.agent/PLANS.md` establish legal ownership, attribution, and operating rules.
`tools/check-license-headers.py` validates applicable tracked files using Git's
NUL-delimited paths; its regression coverage is in
`tests/license_headers/test_check_license_headers.py`; CI invokes both from
`.github/workflows/license-headers.yml`. The plugin implementation is under
`src/main/java/com/totalcross/gradle/`, and the consumer example is
`examples/basic-app/`.

### Decisions and Trade-offs

The validator is dependency-free Python rather than a Gradle task, so the legal
baseline can be checked before plugin implementation is present and in a clean
checkout. Gradle Wrapper files are excluded to preserve their upstream notice.
The example is Linux-only for the minimal reproducible path; platform-specific
testing remains available through plugin configuration.

### Unexpected Problems and Discoveries

The pre-existing headers used a non-canonical `Copyright (C)` form, so they
were replaced with exact SPDX fields. The requested plan used a plural planning
directory that contradicted the repository convention; user confirmation
resolved this in favor of `.agent/`. The initial example settings targeted
multiple platforms, while its Linux-only path produced a complete local package.

### Validation and Measurable Results

`python3 tools/check-license-headers.py` reported `License header validation
passed: 32 applicable files checked.` The Python validator suite ran two tests.
`./gradlew test --console=plain`, `./gradlew check --console=plain`,
`./gradlew publishToMavenLocal --console=plain`, and `./gradlew
sdkSourceNetworkTest --console=plain` completed successfully. The example
deployment reported Retrolambda bytecode version 51 (Java 7) and wrote Linux
artifacts. No performance or artifact-size benchmark was taken.

### Useful Evidence and Examples

The ordered commits `fd4a254`, `bfc3b12`, `7c2f13d`, and `1d513e0` show the
foundation, implementation, example, and compatibility correction. The exact
local reproduction commands are in `README.md` and `CONTRIBUTING.md`. The
validator's expected success count and failure diagnostics are asserted in its
Python tests.

### Limitations, Remaining Work, and Open Questions

The CI workflow has been added but has not been observed on a remote GitHub
run. It uses the repository's normal `python3` availability assumption. The
network check is opt-in because GitHub and S3 availability cannot be made
deterministic. No remote branch was pushed or configured by this plan.

### Possible Article Angles

- For maintainers of newly imported projects: "Starting a repository with a
  legal baseline instead of retroactive cleanup," showing how an initial commit
  can make ownership and validation independently reviewable.
- For build-tool authors: "Testing a Gradle plugin's SDK resolver without
  downloading the SDK," using the opt-in range-request network test as the
  practical takeaway.
- For Java compatibility maintainers: "Keeping a Java 7 deployer path alive
  from a Java 17 Gradle build," explaining the example's Retrolambda evidence.

### Suggested Narrative

Start with the risk of an uncommitted project lacking clear legal metadata.
Explain the decision to create a dependency-free validator and CI before source
history, then introduce the cohesive plugin commit and the separately runnable
example. Show the observed header count and Linux package artifacts, then close
with the deliberate limits: no invented history, no remote push, and an opt-in
network check.

### Claims Requiring Human Review

Confirm legal ownership and contributor policy before making external legal
statements. Confirm the CI workflow result after the repository is pushed.

## Context and Orientation

Before modifying files, inspect the repository rather than assuming that an empty remote means an empty working tree.

Start with:

    pwd
    git rev-parse --show-toplevel
    git status --short --branch
    git remote -v
    git log --oneline --decorate --all --max-count=20
    find .. -name AGENTS.md -o -path '*/.agent/PLANS.md' -o -name PLANS.md
    find . -maxdepth 4 -type f \
      ! -path './.git/*' \
      | sort | sed -n '1,320p'

Read all applicable existing `AGENTS.md` and planning files before editing anything. If they conflict with this ExecPlan, record the conflict and resolve it explicitly rather than silently choosing one.

Confirm freshness:

    git rev-list --all --count
    git show-ref --heads --tags
    git ls-remote --heads --tags origin 2>/dev/null || true

A repository may be treated as fresh when there is no meaningful published project history to preserve. If commits already exist locally or remotely, do not delete, squash, replace, amend, reset, rebase, or force-push them without a separately documented reason and explicit authorization.

Inspect licensing and attribution candidates:

    find . -maxdepth 4 -type f \( \
      -iname 'LICENSE*' -o \
      -iname 'COPYING*' -o \
      -iname 'NOTICE*' -o \
      -iname 'AUTHORS*' -o \
      -iname 'README*' -o \
      -iname 'CONTRIBUTING*' -o \
      -iname 'CODEOWNERS' -o \
      -iname 'AGENTS.md' -o \
      -path '*/.agent/PLANS.md' \
    \) -print

Inspect existing legal markers:

    git grep -n -I -E \
      'Copyright|SPDX-FileCopyrightText|SPDX-License-Identifier|Apache License|Apache-2.0' \
      -- . \
      ':(exclude).git/**' \
      ':(exclude)build/**' \
      ':(exclude)dist/**' \
      ':(exclude)out/**' \
      ':(exclude)target/**' \
      ':(exclude)node_modules/**' \
      || true

For untracked files not visible to `git grep`, use a bounded filesystem search while excluding generated and dependency directories.

Inspect CI and build entry points:

    find .github -maxdepth 4 -type f -print 2>/dev/null | sort
    find . -maxdepth 3 -type f \( \
      -name 'build.gradle' -o \
      -name 'build.gradle.kts' -o \
      -name 'settings.gradle' -o \
      -name 'settings.gradle.kts' -o \
      -name 'gradlew' -o \
      -name 'CMakeLists.txt' -o \
      -name 'Makefile' -o \
      -name 'pom.xml' -o \
      -name 'package.json' -o \
      -name 'Cargo.toml' \
    \) -print | sort

Do not assume that every text file should receive a header. Classify files first.

Normally applicable:

- first-party Java, Kotlin, C, C++, Objective-C, Objective-C++, Rust, Python, shell, Gradle, Groovy, JavaScript, TypeScript, and similar implementation files;
- first-party build scripts whose syntax supports comments;
- first-party test and example sources;
- first-party scripts and reusable templates.

Normally excluded:

- vendored, subtree-managed, generated, or externally imported files whose notices must remain unchanged;
- dependency directories and build outputs;
- lockfiles, binary files, compressed files, patches, minified assets, snapshots, and copied external fixtures;
- license texts themselves;
- plain data formats where comments are invalid or unsafe;
- files explicitly excluded by repository policy.

## Required First Commit: Repository Foundation

The first commit must contain repository foundation only. It must not include application, library, example, test implementation, generated output, or unrelated project source files.

Expected contents, adjusted to the actual repository:

    LICENSE
    NOTICE                         # include when required or useful for Apache notices
    README.md
    AUTHORS.md
    CONTRIBUTING.md
    AGENTS.md
    .agent/PLANS.md
    .github/CODEOWNERS
    .github/workflows/license-headers.yml
    tools/check-license-headers.py # or repository-appropriate equivalent
    tests/license-headers/...      # or lightweight fixtures/test script

If a project-specific build file is strictly necessary to execute the validator tests, avoid adding the entire project build merely to satisfy the first commit. Prefer a dependency-free validator test command. If that is impossible, document the exception and include only the minimal foundation needed.

Before committing:

    git status --short
    git diff --check
    git diff --cached --stat
    git diff --cached

Verify that the staged set contains only repository-foundation files:

    git diff --cached --name-only | sort

Recommended first commit message:

    chore(repo): establish licensing and governance

The commit body should explain that it establishes Apache-2.0 licensing, SPDX policy, attribution, contributor guidance, agent instructions, planning conventions, CODEOWNERS, and automated validation before project implementation is introduced.

After committing:

    git show --stat --oneline HEAD
    git show --name-status --format=fuller HEAD

Do not amend this commit merely to fold in later project implementation. If a foundation defect is found before publication and amendment is permitted, record the reason. Otherwise, fix it in a focused follow-up commit.

## Apache-2.0 License Files

Use the complete, unmodified Apache License 2.0 text in `LICENSE`.

Add `NOTICE` when the project has notices that downstream redistributors must preserve, or when a concise project notice is useful. Do not claim that Apache-2.0 universally requires every project to have a `NOTICE` file. If created, keep it factual and avoid adding restrictions not present in the license.

A suitable initial notice may identify:

    <Project Name>
    Copyright 2026 Amalgam Solucoes em TI Ltda.

Do not place contributor rules, warranty changes, extra license conditions, or branding restrictions in `NOTICE`.

## Canonical SPDX Headers

Use the shortest valid comment syntax while preserving mandatory first-line constructs.

### C-style files

For Java, Kotlin, C, C++, Objective-C, Objective-C++, JavaScript, TypeScript, Gradle, Groovy, and similar formats:

    /*
     * SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
     * SPDX-License-Identifier: Apache-2.0
     */

### Hash-comment files

For shell, Python, Ruby, YAML, and similar formats:

    # SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
    # SPDX-License-Identifier: Apache-2.0

Preserve shebangs as the first line:

    #!/usr/bin/env bash
    # SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
    # SPDX-License-Identifier: Apache-2.0

Preserve Python encoding declarations in their required location.

### XML-style files

Preserve an XML declaration as the first line, then use:

    <!--
      SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
      SPDX-License-Identifier: Apache-2.0
    -->

Do not add comments to formats where comments alter semantics or are unsupported.

## Repository-Level Attribution

### AUTHORS.md

Create concise attribution equivalent to:

    # Authors

    ## Creator and Lead Maintainer

    Fabio Sobral
    GitHub: [@flsobral](https://github.com/flsobral)

    ## Copyright holder

    Amalgam Solucoes em TI Ltda.

    ## Contributors

    Additional contributors are recorded in the Git history.

    Copyright ownership and project authorship are separate concepts. Unless
    explicitly stated otherwise, contributions are made available under the
    Apache License 2.0 applicable to this repository.

Do not claim copyright assignment, a contributor license agreement, or employer ownership without authoritative documentation.

### README.md

The first commit must include a useful README, not merely a placeholder. At minimum include:

- project name and concise purpose;
- current project status, especially if bootstrap or experimental;
- supported or planned platforms only when grounded in the current project scope;
- basic repository/build orientation that is already known;
- license statement;
- creator and maintainer attribution;
- contribution pointer;
- validation command.

Use wording equivalent to:

    ## Maintainer

    Created and maintained by [Fabio Sobral](https://github.com/flsobral).

    Copyright © 2026 Amalgam Solucoes em TI Ltda.

    Licensed under the Apache License 2.0.

Do not invent features, support guarantees, release status, compatibility, or build instructions that cannot yet be verified.

### CODEOWNERS

Create `.github/CODEOWNERS` with a repository-wide default:

    * @flsobral

Add path-specific rules only when the current repository structure justifies them. Do not enable branch protection or mandatory code-owner review unless separately requested.

## CONTRIBUTING.md

Document:

- how to build and test using verified commands;
- how to run the license-header validator;
- required SPDX metadata for new first-party source files;
- preservation of shebangs and declarations;
- treatment of generated, vendored, imported, and separately licensed code;
- expectations for focused, descriptive commits;
- a rule against inventing historical context when importing an existing working tree into the new repository.

Canonical metadata:

    SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
    SPDX-License-Identifier: Apache-2.0

## AGENTS.md

Create a root `AGENTS.md` that gives coding agents concise, repository-specific operational guidance. It should:

- identify authoritative build, test, format, and validation commands once known;
- require reading `.agent/PLANS.md` for substantial work;
- require preservation of Apache-2.0 and third-party notices;
- prohibit editing generated or vendored files unless the task explicitly requires it;
- require concise tool output and focused validation;
- require inspection of `git status` before and after changes;
- prohibit destructive Git operations, force pushes, history rewriting, or commit amendment without explicit authorization;
- require contextual commits when committing is requested, while prohibiting fabricated history;
- identify directory-specific instructions when subdirectory `AGENTS.md` files are later added.

Do not include speculative architecture instructions. Ground them in the current repository or clearly mark them as future decisions.

## .agent/PLANS.md

Create `.agent/PLANS.md` in the first commit as the authoritative repository template and policy for future Codex ExecPlans.

It must explain:

- when an ExecPlan is required;
- that plans are living documents;
- required sections such as `Purpose / Big Picture`, `Progress`, `Surprises & Discoveries`, `Decision Log`, `Outcomes & Retrospective`, context, implementation steps, validation, and recovery;
- that every command and path must be repository-specific and executable;
- that plans must define observable acceptance criteria;
- that agents must update progress and decisions during execution;
- that plans must remain self-contained and understandable without external chat context;
- that destructive or irreversible operations need explicit safeguards and authorization;
- that commit plans must prefer truthful current context over reconstructed or invented chronology.

The current ExecPlan may be stored in `.agent/` as a separate plan file if appropriate, but `.agent/PLANS.md` is the policy/template, not the implementation plan itself.

## Validation Tool

Prefer a small dependency-free script in a language already available in CI. Python is preferred when available.

The validator must:

1. obtain candidate files from Git using NUL-delimited paths;
2. inspect tracked files and optionally staged files before the first implementation commit;
3. apply explicit inclusion and exclusion rules;
4. detect vendored, imported, generated, and separately licensed paths;
5. verify the exact copyright line;
6. verify `SPDX-License-Identifier: Apache-2.0`;
7. identify obsolete or conflicting first-party legal notices;
8. print one concise diagnostic per failing file;
9. return zero on success and nonzero on failure;
10. sort diagnostics deterministically;
11. support spaces and nontrivial path names;
12. never rewrite files during validation;
13. keep successful output concise for CI and agent consumption.

Use:

    git ls-files -z

During the bootstrap, applicable untracked files may need a separate migration command or a staged-files mode because `git ls-files` does not include them until staged. Keep mutation and validation as separate operations.

Example diagnostics:

    path/to/File.java: missing SPDX-FileCopyrightText
    path/to/File.java: expected copyright holder "2026 Amalgam Solucoes em TI Ltda."
    path/to/File.java: expected SPDX license "Apache-2.0"

Example success output:

    License header validation passed: 84 applicable files checked.

## Validator Tests

At minimum cover:

1. valid C-style header;
2. valid hash-comment header;
3. shebang followed by a valid header;
4. XML declaration followed by a valid header;
5. missing copyright line;
6. missing license line;
7. misspelled company name;
8. wrong year;
9. a license identifier other than `Apache-2.0`;
10. third-party exclusion;
11. generated-file exclusion;
12. path containing spaces;
13. deterministic sorted diagnostics;
14. staged or bootstrap file handling if supported.

Use a dependency-free test script unless the repository already has an appropriate test framework that can be included without pulling project implementation into the first commit.

## CI Integration

Create a focused workflow in the first commit. Reuse repository conventions if any already exist.

Use a clear job name such as:

    license-headers

The workflow must invoke the same command documented in `README.md` and `CONTRIBUTING.md`. Keep logs concise. Trigger it for pull requests and relevant pushes, with path filtering only if the filter cannot accidentally skip files requiring validation.

## Preparing Remaining Project Files

After the first commit, classify all remaining files.

Create a working inventory containing at least:

- path;
- category;
- first-party, third-party, generated, or uncertain status;
- applicable SPDX comment syntax;
- functional area;
- direct dependencies;
- likely commit group;
- evidence supporting that group;
- unresolved questions.

The inventory may be temporary, but material decisions and uncertainties must be copied into this plan's `Surprises & Discoveries` or `Decision Log`.

Apply SPDX headers only to applicable first-party files. Do not change third-party content to make it appear first-party.

## Contextual Commit Strategy

The objective is not to reconstruct the unknowable development chronology. It is to create a clean, reviewable initial history that reflects the current logical organization of the project.

Use evidence such as:

- build graph and module boundaries;
- source package or namespace structure;
- platform directories;
- tests tied directly to implementation;
- documentation tied to a feature;
- dependency direction;
- cohesive APIs and implementations;
- generated versus hand-maintained boundaries;
- commit-worthy configuration changes with a clear independent purpose.

Good commit groups may include, when supported by the repository:

- build/bootstrap infrastructure;
- core abstractions and public API;
- a specific platform backend;
- rendering or windowing backend;
- examples and demos;
- tests for the implementation introduced in the same or immediately preceding commit;
- documentation for a concrete feature;
- CI beyond the foundation validator.

Each contextual commit must:

1. be internally coherent;
2. avoid knowingly broken intermediate states when practical;
3. include directly associated tests and documentation where that improves reviewability;
4. use a descriptive conventional commit message if that matches repository policy;
5. include a body explaining why the grouped files belong together when the grouping is not obvious;
6. avoid assertions such as “initial implementation of X” unless the commit truly introduces X in this repository history;
7. avoid dates, authors, or motivations inferred without evidence.

Before each commit:

    git status --short
    git diff --check
    git diff --cached --stat
    git diff --cached

After each commit:

    git show --stat --oneline HEAD

Record the commit hash, message, included functional area, and grouping rationale in `Outcomes & Retrospective`.

## Fallback: Single Project Commit

Use the fallback when:

- files are tightly coupled and splitting would create misleading or broken states;
- the current tree lacks enough evidence to identify independent contexts;
- imported files have lost their original history and no authoritative upstream mapping exists;
- generated and source relationships cannot yet be safely separated;
- an attempted split would require inventing rationale.

Recommended message:

    feat: add initial project implementation

The commit body should state that the repository was initialized from the current project tree and that a finer historical decomposition was intentionally avoided because reliable original context was unavailable.

Do not describe this fallback as a failure. Truthful provenance is an explicit acceptance criterion.

## Validation and Acceptance

The implementation is complete only when all of the following are observable:

- no history rewrite was performed;
- the first commit contains the complete repository legal and operational foundation and no project implementation files;
- `LICENSE` contains the complete Apache License 2.0 text;
- `NOTICE`, if present, contains only appropriate factual notices;
- every applicable first-party source file contains the exact required SPDX lines;
- third-party, vendored, generated, and separately licensed files retain their authoritative notices;
- `README.md` accurately describes the current project without invented claims;
- `AUTHORS.md` identifies Fabio Sobral as Creator and Lead Maintainer and Amalgam Solucoes em TI Ltda. as copyright holder;
- `.github/CODEOWNERS` contains the appropriate default rule for `@flsobral`;
- `AGENTS.md` provides current repository-operating guidance;
- `.agent/PLANS.md` defines the repository's ExecPlan requirements;
- the documented local validator command succeeds from a clean checkout;
- CI executes the same validator;
- validator tests pass;
- normal focused build and project tests pass;
- project files are committed in truthful contextual groups, or the documented fallback commit is used;
- the final tree contains no unrelated local files or build output;
- `git diff --check` passes;
- the final working tree is clean unless explicitly documented otherwise.

## Final Review Checklist

- [ ] The repository was confirmed to be fresh, or any pre-existing history was preserved unchanged.
- [ ] No rebase, filter, reset, amend, force push, or history rewrite was performed without separate authorization.
- [ ] The first commit contains only repository foundation files.
- [ ] The first commit includes `LICENSE`, README, attribution, contributor guidance, validation, CI, `AGENTS.md`, and `.agent/PLANS.md`.
- [ ] The authoritative license is Apache License 2.0.
- [ ] Every applicable first-party file uses `SPDX-License-Identifier: Apache-2.0`.
- [ ] The company name is exactly `Amalgam Solucoes em TI Ltda.`.
- [ ] The year is exactly `2026` unless an authoritative exception is documented.
- [ ] Fabio Sobral is described as creator and lead maintainer, not as the company copyright holder.
- [ ] Third-party and separately licensed notices were preserved.
- [ ] Generated files were not manually edited unless explicitly required.
- [ ] New-file SPDX rules are documented.
- [ ] The validator is deterministic and dependency-light.
- [ ] Validator tests pass.
- [ ] CI invokes the same documented validator command.
- [ ] The README contains only verified project claims and commands.
- [ ] Contextual commit groups are supported by current-tree evidence.
- [ ] No commit message invents historical chronology or motivation.
- [ ] The fallback single project commit was used when a reliable split was not possible.
- [ ] Focused project builds and tests pass.
- [ ] `git diff --check` passes.
- [ ] The final commit sequence is recorded in `Outcomes & Retrospective`.
- [ ] The final working tree is clean or remaining files are explicitly accounted for.

## Recovery and Safety

Because no history rewrite is required, recovery should rely on ordinary forward commits rather than destructive Git operations.

Before starting, record:

    git status --short --branch
    git remote -v
    git log --oneline --decorate --all --max-count=20

Do not remove untracked files merely because they are not part of a commit. Inventory them first. Do not commit credentials, local IDE state, signing material, build output, caches, or machine-specific configuration.

If an incorrect file is staged, unstage it without discarding its content. If an incorrect commit has already been published, correct it with a new commit unless explicit authorization is given for another approach.

Pushing commits, changing repository visibility, configuring branch protection, creating releases, or modifying remote settings are outside this plan unless separately requested.

Revision 2026-07-15: executed the bootstrap, corrected the planning-directory
reference to the user-confirmed `.agent/` path, recorded the created commits,
and replaced placeholders with observed validation and editorial evidence.
