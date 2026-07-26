// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import com.totalcross.tooling.platform.HostPlatform;
import com.totalcross.tooling.process.ProcessRequest;
import com.totalcross.tooling.process.ProcessResult;
import com.totalcross.tooling.process.ProcessRunner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Proves that a JDK can run Java, javac, child processes, and optional tools. */
public final class JdkCapabilityProbe {
    private final HostPlatform platform;
    private final ProcessRunner runner;
    private final Duration timeout;

    public JdkCapabilityProbe(HostPlatform platform) {
        this(platform, new ProcessRunner(), Duration.ofSeconds(15));
    }

    JdkCapabilityProbe(HostPlatform platform, ProcessRunner runner, Duration timeout) {
        this.platform = platform;
        this.runner = runner;
        this.timeout = timeout;
    }

    public JdkCapabilityReport probe(JdkInstallation installation, Path protoc) {
        List<String> failures = new ArrayList<>();
        Path home = installation.home();
        if (home == null) return report(installation, failures, "home is not set");
        Path java = executable(home, "java");
        Path javac = executable(home, "javac");
        if (!Files.isRegularFile(java)) failures.add("missing " + java);
        if (!Files.isRegularFile(javac)) failures.add("missing " + javac);
        if (failures.isEmpty()) {
            check(failures, "java -version", List.of(java.toString(), "-version"));
            check(failures, "javac -version", List.of(javac.toString(), "-version"));
            check(failures, "Java child process", List.of(java.toString(), "-version"));
        }
        if (platform.operatingSystem() == HostPlatform.OperatingSystem.MACOS) {
            checkMacXattr(failures, home);
        }
        if (protoc != null) check(failures, "protoc --version", List.of(protoc.toString(), "--version"));
        return new JdkCapabilityReport(installation, failures);
    }

    private void checkMacXattr(List<String> failures, Path home) {
        try {
            ProcessResult result = runner.run(new ProcessRequest(List.of("/usr/bin/xattr", "-p",
                    "com.apple.quarantine", home.toString()), null, null, timeout));
            if (result.timedOut()) failures.add("xattr timed out");
        } catch (IOException | InterruptedException error) {
            failures.add("xattr could not run: " + error.getMessage());
        }
    }

    private void check(List<String> failures, String name, List<String> command) {
        try {
            ProcessResult result = runner.run(new ProcessRequest(command, null, null, timeout));
            if (!result.succeeded()) failures.add(name + " failed with exit " + result.exitCode() + ": "
                    + concise(result.stderr(), result.stdout()));
        } catch (IOException | InterruptedException error) {
            failures.add(name + " could not run: " + error.getMessage());
        }
    }

    private static Path executable(Path home, String name) {
        return home.resolve("bin").resolve(name + (PlatformNames.isWindows() ? ".exe" : ""));
    }

    private static String concise(String first, String second) {
        String value = first.isBlank() ? second : first;
        return value.replace('\n', ' ').trim();
    }

    private static JdkCapabilityReport report(JdkInstallation installation, List<String> failures, String failure) {
        failures.add(failure);
        return new JdkCapabilityReport(installation, failures);
    }

    private static final class PlatformNames {
        private static boolean isWindows() {
            return System.getProperty("os.name").toLowerCase().startsWith("windows");
        }
    }
}
