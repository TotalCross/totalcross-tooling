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
- A TotalCross SDK version that exposes the Live Preview runtime contract
- A TotalCross Live Preview Server distribution
- Node.js and npm compatible with this extension's dependencies when building from source

## Use

Open the Command Palette (`F1` or `Cmd+Shift+P`) and select one of these
commands:

- `TotalCross: Create new Project`
- `TotalCross: Package`
- `TotalCross: Deploy`
- `TotalCross: Deploy&Run`
- `TotalCross: Convert Maven Project to Gradle`
- `TotalCross: Start Preview`
- `TotalCross: Open Live Preview` (an alias for Start Preview)
- `TotalCross: Reload Preview`
- `TotalCross: Stop Preview`
- `TotalCross: Open Preview Config`

The existing `TotalCross: Preview` and `TotalCross: Run` commands remain the
build-tool workflow for Gradle and Maven projects. The commands above are the
visual Live Preview workflow and open the browser-like panel inside VS Code.

## Live Preview

Live Preview is a local, read-only image of a compiled TotalCross Java user
interface. Start it from the Command Palette in a single-folder workspace. The
extension creates `totalcross.preview.json` in the workspace if it does not
exist, then finds classes extending `totalcross.ui.MainWindow` and asks which
one to use. It opens a panel beside the editor and starts the SDK service on
`127.0.0.1` using an automatically selected port.

The extension does not compile Java sources. Configure Java tooling or Gradle
to compile on save and keep `classOutputPaths` in the preview configuration
pointing to the emitted `.class` files. Focusing a compiled Java
`MainWindow`, `Container`, or `Control` asks the service to show that
class; a non-compiled or incompatible source clears the panel. Reload uses the
configured fast reload when possible and restarts the service for
`reloadMode: "full"` or a failed fast reload.

For Gradle projects, the generated configuration starts with these
project-relative paths:

    {
      "mainWindow": "",
      "launcherArgs": ["width", "500", "height", "600"],
      "classOutputPaths": ["build/classes/java/main"],
      "resourcePaths": ["src/main/resources"],
      "dependencyPaths": ["build/libs", "lib"],
      "previewMode": "windowed",
      "reloadMode": "fast",
      "width": 500,
      "height": 600,
      "scale": 1,
      "platform": "android",
      "headlessOutput": "build/totalcross-preview/preview.png"
    }

For a Maven workspace, the generated configuration uses `target/classes` and
`mvn compile`; an existing `totalcross.preview.json` is preserved and takes
precedence.

Set `totalcross.livePreview.extraClasspath` in workspace settings to the `lib`
directory of the published or locally installed TotalCross Live Preview Server
distribution. That directory contains the server jar and its SDK dependency;
do not depend on an uncommitted SDK checkout. For example, replace the
placeholder with the installed distribution path:

    {
      "totalcross.livePreview.extraClasspath": [
        "/opt/totalcross-live-preview-server/lib"
      ]
    }

The service is always local to the VS Code machine and is launched with
structured Java arguments rather than a shell command. Preview does not forward
mouse, touch, keyboard, or navigation events. On macOS the extension adds
`-Dapple.awt.UIElement=true` unless it is already present in
`totalcross.livePreview.jvmArgs`.

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
`pom.xml.maven-backup` only after that validation succeeds. If the unpublished
plugin is missing from Maven Local, the generated Gradle files and original POM
remain so that publishing the plugin and retrying the command is safe.

When `totalcross.preview.json` already exists, conversion updates its build
command and Gradle output paths while preserving the selected MainWindow and
other preview preferences. Maven compiler excludes and additional active Maven
dependencies are carried into the generated `build.gradle`.

The conversion writes activation keys only to project-local `gradle.properties`,
which it adds to `.gitignore`; it does not put the key in `build.gradle` or
`.totalcross/project.json`. A root with unrelated Maven and Gradle files is
deliberately not packaged or deployed automatically, because the extension
cannot know which build is authoritative.

For a project configured with the `linux_arm` platform, the Gradle plugin writes
the SSH deployer's install directory beneath
`build/totalcross/install/linux_arm`, so `TotalCross: Deploy` uses that output
after packaging.

## Using the unpublished Gradle plugin locally

Before creating a project with the default `0.1.0-SNAPSHOT` plugin version,
publish the plugin checkout to Maven Local:

    ./gradlew clean test publishToMavenLocal --console=plain

Run that command from `totalcross-gradle-plugin`. Generated `settings.gradle`
files search Maven Local before public plugin repositories, so Gradle can find
the local plugin marker and implementation. The
`totalcross.gradlePluginVersion` VS Code setting must match the version that
was published locally.

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
