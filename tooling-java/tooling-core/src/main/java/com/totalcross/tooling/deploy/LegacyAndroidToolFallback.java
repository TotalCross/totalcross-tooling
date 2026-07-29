// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.deploy;

import com.totalcross.tooling.process.ProcessRequest;
import com.totalcross.tooling.process.ProcessRunner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/** Read-only compatibility lookup for tools already present in an explicitly selected SDK. */
public final class LegacyAndroidToolFallback {
    public record Tools(Path protoc, Path bundletool) {}

    private final ProcessRunner processes;

    public LegacyAndroidToolFallback() {
        this(new ProcessRunner());
    }

    LegacyAndroidToolFallback(ProcessRunner processes) {
        this.processes = processes;
    }

    public Optional<Tools> find(Path sdk, Path javaHome) {
        if (sdk == null) return Optional.empty();
        Path root = sdk.resolve("etc").resolve("tools").resolve("android");
        String executable = System.getProperty("os.name", "").toLowerCase().startsWith("windows") ? "protoc.exe" : "protoc";
        Path protoc = root.resolve("protoc").resolve("bin").resolve(executable);
        Path bundletool = firstBundletool(root);
        if (!Files.isRegularFile(protoc) || bundletool == null || !probe(List.of(protoc.toString(), "--version"), "21.0")) {
            return Optional.empty();
        }
        if (!probe(List.of(javaExecutable(javaHome).toString(), "-jar", bundletool.toString(), "version"), null)) {
            return Optional.empty();
        }
        return Optional.of(new Tools(protoc, bundletool));
    }

    private Path firstBundletool(Path root) {
        try (var paths = Files.list(root)) {
            return paths.filter(path -> path.getFileName().toString().startsWith("bundletool-all-")
                            && path.getFileName().toString().endsWith(".jar"))
                    .filter(Files::isRegularFile).findFirst().orElse(null);
        } catch (IOException ignored) {
            return null;
        }
    }

    private boolean probe(List<String> command, String expected) {
        try {
            var result = processes.run(new ProcessRequest(command, null, null, Duration.ofSeconds(30)));
            String output = result.stdout() + " " + result.stderr();
            return result.succeeded() && !output.isBlank() && (expected == null || output.contains(expected));
        } catch (IOException | InterruptedException failure) {
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            return false;
        }
    }

    private static Path javaExecutable(Path javaHome) {
        if (javaHome == null) return Path.of("java");
        String suffix = System.getProperty("os.name", "").toLowerCase().startsWith("windows") ? ".exe" : "";
        Path candidate = javaHome.resolve("bin").resolve("java" + suffix);
        return Files.isRegularFile(candidate) ? candidate : Path.of("java");
    }
}
