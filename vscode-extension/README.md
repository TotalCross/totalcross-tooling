<!--
Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.
Copyright (C) 2022-2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->

> [!IMPORTANT]
> This project has moved to the
> [`TotalCross/totalcross-tooling`](https://github.com/TotalCross/totalcross-tooling)
> repository under [`vscode-extension/`](https://github.com/TotalCross/totalcross-tooling/tree/main/vscode-extension).
> Development continues there. This repository is retained for historical
> issues, pull requests, releases, and links and is intended to be archived.

# TotalCross VS Code Extension

This extension adds commands to Visual Studio Code for creating, packaging,
deploying, and deploying-and-running TotalCross projects. Deploy-and-run is
currently intended for Linux ARM targets reached over SSH.

## Requirements

- Java JDK 17
- Visual Studio Code 1.85 or newer
- A TotalCross SDK version that exposes the preview runtime contract
- Node.js and npm compatible with this extension's dependencies when building from source

## Use

Open the Command Palette (`F1` or `Cmd+Shift+P`) and select one of these
commands:

- `TotalCross: Create new Project`
- `TotalCross: Package`
- `TotalCross: Deploy`
- `TotalCross: Deploy&Run`
- `TotalCross: Convert Maven Project to Gradle`
- `TotalCross: Preview`
- `TotalCross: Reload Preview`
- `TotalCross: Stop Preview`
- `TotalCross: Select Preview MainWindow`
- `TotalCross: Open Preview Config`

`TotalCross: Preview` and `TotalCross: Run` use the authenticated Java
host/worker pipeline supplied by the Gradle or Maven plugin. Preview opens a
panel beside the editor; closing that panel stops the coordinator and its
workers.

## Live Preview

Preview renders the compiled class selected in the active Java editor. The
source must be inside the selected workspace, have a top-level class, and have
an emitted `.class` file. The worker accepts `totalcross.ui.MainWindow`,
`Container`, and `Control` targets. An uncompiled or incompatible class clears
the panel and leaves the previous healthy worker untouched while the candidate
is rejected. After a successful build, the active class is presented again.

The project-owned `totalcross-preview.json` contains the canonical fields below;
unknown fields are preserved when the extension edits the file:

    {
      "mainWindow": "",
      "launcherArgs": [],
      "classpath": []
    }

The generated `project-model.json` remains authoritative for class output,
resources, and dependencies. `classpath` adds project-relative or absolute
entries. Older `classOutputPaths`, `resourcePaths`, and `dependencyPaths` are
read as compatibility additions; HTTP-only fields are ignored. Configure
`totalcross.livePreview.jvmArgs` for extra JVM options. Preview always adds
headless mode, and on macOS also adds `-Dapple.awt.UIElement=true`.

`Select Preview MainWindow` discovers classes extending `MainWindow`, prefers a
class referenced by `TotalCrossApplication.run`, selects the only candidate
automatically, or opens a Quick Pick for ambiguity. `Reload Preview` builds and
re-presents the active Java class, or reloads the configured MainWindow when no
Java target is active. A serialized panel stores its workspace and presentation
class and can restart the same project after a VS Code window reload.

New projects use the Gradle Wrapper included in the generated project, so a
separate Gradle or Maven installation is not required. Package a project with
`TotalCross: Package` or from the project directory with:

    ./gradlew totalcrossPackage

On Windows, use `gradlew.bat totalcrossPackage --console=plain`. Generated
output is rooted at `build/totalcross`. Existing Maven-only projects remain
supported and continue to package with `mvn package` and use `target/install`.

## Migrating a Maven project

When a workspace root contains a TotalCross `pom.xml` and no root Gradle build
files, the extension asks in English whether to convert it. The only actions
are `Convert Now` and `Remind Me Tomorrow`. Dismissing the notification has the
same effect as the reminder action: that workspace is not prompted again for
exactly 24 hours. Other workspace folders have independent reminders.

`Convert Now` reads the TotalCross SDK and plugin configuration from the POM,
creates a marked Groovy Gradle project and Wrapper, and runs `./gradlew tasks
--console=plain`. The Java source tree is unchanged. The POM becomes
`pom.xml.maven-backup` only after that validation succeeds. If the configured
release plugin cannot be resolved, the generated Gradle files and original POM
remain so that selecting an available released version and retrying is safe.
The validation also checks that the installed plugin exposes the
`totalcrossPreview` and `totalcrossRun` tasks used by the extension.

When `totalcross-preview.json` already exists, conversion updates its build
command and Gradle output paths while preserving the selected MainWindow and
other preview preferences. Maven compiler excludes and additional active Maven
dependencies are carried into the generated `build.gradle`.
Legacy Eclipse/Maven metadata (`.classpath`, `.project`, and the generated
`.settings` entries) is removed after successful validation so the VS Code Java
extension imports the Gradle model instead of retaining the old Maven classpath.

The conversion writes activation keys only to project-local `gradle.properties`,
which it adds to `.gitignore`; it does not put the key in `build.gradle` or
`.totalcross/project.json`. A root with unrelated Maven and Gradle files is
deliberately not packaged or deployed automatically, because the extension
cannot know which build is authoritative.

For a project configured with the `linux_arm` platform, the Gradle plugin writes
the SSH deployer's install directory beneath
`build/totalcross/install/linux_arm`, so `TotalCross: Deploy` uses that output
after packaging.

## Gradle plugin version

New and migrated projects use the released `0.1.0` TotalCross Gradle plugin
through public release repositories. To use a later release, set
`totalcross.gradlePluginVersion` to that published version before creating or
converting the project. Generated projects never add Maven Local implicitly.

## Development

From the repository root, run:

    npm ci
    npm run audit
    npm run compile
    python3 tools/check-repository-governance.py
    python3 -m unittest tests.test_repository_governance

`npm test` runs the extension integration tests after compilation. It may
download and start a compatible VS Code test instance.

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution and validation rules.

## Authors and maintenance

The original creator is [Italo Yeltsin](https://github.com/ItaloYeltsin).
[Fabio Sobral](https://github.com/flsobral) is the sole current maintainer.
Historical contributors are listed in [AUTHORS.md](AUTHORS.md).

## License and transition

The repository's current license is [Apache License 2.0](LICENSE). Versions
and source distributions released before this governance change may remain
available under the MIT License terms that accompanied them. From this
governance change onward, project work controlled by the current copyright
holder is licensed under the Apache License, Version 2.0, unless a file states
otherwise.

Historical TotalCross source files retain their MIT notices and licensing.
See [NOTICE](NOTICE) for attribution and provenance details.
