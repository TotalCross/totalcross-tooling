<!--
SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: Apache-2.0
-->

# TotalCross Gradle Plugins

The `com.totalcross.application` and `com.totalcross.library` plugins package Java
TotalCross applications and libraries with `tc.Deploy`.
It finds the `totalcross-sdk` runtime dependency, first looks for the matching SDK
archive in the `TotalCross/totalcross` GitHub release, and falls back to the historic
S3 release URL when GitHub does not have it or is unavailable. It then chooses the
required Zulu runtime automatically. SDK 7.3.0 and newer use JDK 17; earlier SDKs use
JDK 11, matching the historic Maven-plugin behavior.
Retrolambda 2.5.7 is used only for an earlier SDK with Java 8 bytecode, where it
converts the application to Java 7 before deployment.

## Local development and the example

Run the bundled example from the repository root:

    ./gradlew -p examples/basic-app clean totalcrossPackage --info

The first run downloads the SDK and JDK into Gradle's user home. It produces
`examples/basic-app/build/totalcross/MainWindow.tcz`.

The example uses a composite build (`includeBuild('../..')`) so no publication is
needed while developing the plugin. To also publish it locally, run:

    ./gradlew publishToMavenLocal

This publishes `com.totalcross:totalcross-gradle-plugin:0.1.0-SNAPSHOT` and the
markers for `com.totalcross.application` and `com.totalcross.library`.

## Applying the plugin

    plugins {
        id 'java'
        id 'com.totalcross.application' version '0.1.0-SNAPSHOT'
    }

    repositories {
        maven {
            url = uri('http://maven.totalcross.com/artifactory/repo1')
            allowInsecureProtocol = true
        }
        mavenCentral()
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
    }

    dependencies {
        implementation 'com.totalcross:totalcross-sdk:7.2.2'
    }

    totalcross {
        applicationName = 'MainWindow'
        platforms = ['linux']
        activationKey = providers.gradleProperty('totalcrossActivationKey').orNull
        // totalcrossHome = file('/path/to/TotalCross-7.2.2')
        // deploySdkJar = file('/path/to/totalcross-sdk.jar')
        // jdkPath = file('/path/to/jdk-17')
        // certificates = file('/path/to/certificates')
        // externalResources.from('assets')
        // totalcrossLib = true
        // logLevel = 'debug'
        // deployArguments = ['/log-level', 'verbose']
    }

`totalcrossPackage` depends on the standard `jar` task and is attached to
`assemble`. `com.totalcross.library` has the same configuration, but enables
`totalcrossLib` by default and embeds the resulting `*Lib.tcz` in the JAR.
Both plugins accept the same core packaging concepts as the Maven plugin:
application name, platforms, activation key, certificates, a supplied SDK/JDK,
external resources, and TotalCross libraries. External resources and `*Lib.tcz`
libraries are added to the generated `all.pkg` file.

`logLevel` accepts `quiet`, `normal`, `verbose` and `debug`. SDK 7.3.0 or newer
receives `/log-level <level>`; for earlier SDKs, only `verbose` is honored and
is translated to `/v`. Other configured levels are ignored for those SDKs.

Declare platform names without a prefix, such as `platforms = ['linux']`; the
plugin passes them to `tc.Deploy` as `-linux`. A value already starting with `-`
is preserved.

## Testing a rebuilt deployer JAR

Set `deploySdkJar` to run `tc.Deploy` from a rebuilt `totalcross-sdk.jar` ahead
of the JAR inside `totalcrossHome`. This does not publish the JAR or require a
new SDK distribution; `totalcrossHome` still supplies the SDK files such as
`etc` (including `etc/security` for Android) needed by the deployer.

    totalcross {
        deploySdkJar = file('/path/to/totalcross-sdk-7.2.2.jar')
    }

The bundled example accepts the same setting through a Gradle property:

    ./gradlew -p examples/basic-app clean totalcrossPackage \
        -PtestDeploySdkJar=/path/to/totalcross-sdk-7.2.2.jar

When the rebuilt JAR requires Java 17 but its SDK version would normally select
JDK 11, also provide the existing Java-17 SDK test home; the example then sets
`sdkVersion = '7.3.0'` and uses JDK 17:

    ./gradlew -p examples/basic-app clean totalcrossPackage \
        -PtestTotalcrossHome=/path/to/TotalCross \
        -PtestDeploySdkJar=/path/to/totalcross-sdk-7.2.2.jar \
        -PtestJdkPath=/path/to/a/working-jdk-17

## SDK and Java-target compatibility

- SDK `7.3.0` or newer accepts classfile targets through Java 17 and fails above Java 17. It runs `tc.Deploy` with cached JDK 17.
- Earlier SDKs fail above Java 8. At Java 8 the plugin runs Retrolambda 2.5.7 with cached JDK 11 and deploys the resulting Java 7 bytecode with that same JDK.
- Other accepted combinations proceed without Retrolambda.

When a full SDK is supplied instead of the Maven-resolved distribution, set `sdkVersion` to identify its compatibility level:

    totalcross {
        sdkVersion = '7.3.0'
        totalcrossHome = file('/path/to/TotalCross')
    }

The SDK resolver first queries the GitHub release tag `v<version>` and uses its
`TotalCross-<version>.zip` asset. If that release or asset is unavailable, it keeps
the Maven-plugin-compatible S3 URL as fallback. The SDK cache lives in
`<GRADLE_USER_HOME>/caches/totalcross/sdk/<version>`. The plugin maintains
`zulu_jdk_11` and `zulu_jdk_17` independently under
`<GRADLE_USER_HOME>/caches/totalcross/jdk`. Delete only the affected directory and
rerun the task to recover from a failed download. The Zulu resolver excludes CRaC
builds, which are unable to start child processes on current macOS releases.

The normal test suite is offline and deterministic. To verify the external source
selection without downloading an SDK archive, run:

    ./gradlew sdkSourceNetworkTest --console=plain

This opt-in check confirms the GitHub release and starts a one-byte range request for
`7.2.0`; it then confirms that missing GitHub release `5.8.4` starts the same partial
request through the S3 fallback.

## Verifying a newer SDK artifact with Java 17 classfiles

The default resolver continues to use the official TotalCross release URL. To test a
full SDK distribution built elsewhere, pass its extracted root only to the example:

    ./gradlew -p examples/basic-app clean totalcrossPackage \
        -PtestTotalcrossHome=/path/to/TotalCross

This opt-in test compiles the example to Java 17 classfile format and prioritizes the
selected SDK's `dist/totalcross-sdk.jar` for `tc.Deploy`. It does not change normal
SDK resolution or inject ASM into the deployer classpath.
The bundled test marks the Actions artifact as `sdkVersion = '7.3.0'`.

## Validation

Validate first-party license metadata without modifying files:

    python3 tools/check-license-headers.py

Run its dependency-free regression tests with:

    python3 tests/license_headers/test_check_license_headers.py

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for local validation, SPDX requirements,
and contribution guidance.

## Maintainer

Created and maintained by [Fabio Sobral](https://github.com/flsobral).

Copyright © 2026 Amalgam Solucoes em TI Ltda.

Licensed under the [Apache License 2.0](LICENSE).
