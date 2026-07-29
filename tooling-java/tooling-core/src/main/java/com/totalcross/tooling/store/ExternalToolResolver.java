// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.store;

import com.totalcross.tooling.platform.HostPlatform;
import com.totalcross.tooling.platform.StoreLayout;
import com.totalcross.tooling.process.ProcessRequest;
import com.totalcross.tooling.process.ProcessResult;
import com.totalcross.tooling.process.ProcessRunner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** Resolves immutable Android deployment tools in the shared, vendor-neutral store. */
public final class ExternalToolResolver {
    private final StoreLayout layout;
    private final com.totalcross.tooling.download.Downloader downloader;
    private final ArtifactInstaller installer;
    private final FileLockManager locks;
    private final ProcessRunner processes;
    private final boolean offline;

    public ExternalToolResolver() {
        this(StoreLayout.detect(), new com.totalcross.tooling.download.HttpDownloader(), false);
    }

    public ExternalToolResolver(StoreLayout layout, com.totalcross.tooling.download.Downloader downloader,
                                boolean offline) {
        this(layout, downloader, new ArtifactInstaller(layout), new FileLockManager(), new ProcessRunner(), offline);
    }

    ExternalToolResolver(StoreLayout layout, com.totalcross.tooling.download.Downloader downloader,
                         ArtifactInstaller installer, FileLockManager locks, ProcessRunner processes,
                         boolean offline) {
        this.layout = layout;
        this.downloader = downloader;
        this.installer = installer;
        this.locks = locks;
        this.processes = processes;
        this.offline = offline;
    }

    public Path resolve(String name, Path javaHome) throws IOException {
        ExternalToolCatalog.Entry entry = ExternalToolCatalog.entry(name, layout.platform());
        if (entry == null) {
            throw new IOException("Unsupported external tool or host platform: " + name + " on " + layout.platform().id());
        }
        return resolve(entry, javaHome);
    }

    /** Resolves a concrete catalog entry; exposed for deterministic store tests and mirrors the public name lookup. */
    public Path resolve(ExternalToolCatalog.Entry entry, Path javaHome) throws IOException {
        ArtifactCoordinate coordinate = new ArtifactCoordinate("tools", entry.name(), entry.version(), entry.build());
        Path target = layout.externalToolRoot(entry.name(), entry.version(), entry.build());
        Path payload = payload(target, entry);
        if (Files.isRegularFile(target.resolve(".totalcross-install-complete")) && Files.isRegularFile(payload)) {
            prepareExecutable(payload);
            verify(entry, payload, javaHome);
            return executable(payload, entry);
        }
        if (offline) throw new IOException("Offline mode: external tool is not installed: " + coordinate.id());

        Path archive = cachedArchive(entry);
        Path downloadLock = layout.cacheRoot().resolve("downloads").resolve(entry.name() + "-" + entry.version() + ".lock");
        try (FileLockManager.Lock ignored = locks.acquire(downloadLock)) {
            if (!Files.isRegularFile(archive)) {
                downloader.download(new com.totalcross.tooling.download.DownloadRequest(entry.source(), archive, entry.sha256()));
            } else {
                try {
                    ChecksumVerifier.verify(archive, entry.sha256());
                } catch (IOException mismatch) {
                    if (offline) throw mismatch;
                    Files.deleteIfExists(archive);
                    downloader.download(new com.totalcross.tooling.download.DownloadRequest(entry.source(), archive, entry.sha256()));
                }
            }
        }
        InstallRequest request = new InstallRequest(coordinate, archive, entry.source(), entry.sha256(), layout.platform());
        InstalledArtifact installed = entry.name().equals("bundletool")
                ? installer.installFile(request, entry.assetName())
                : installer.install(request);
        payload = payload(installed.path(), entry);
        prepareExecutable(payload);
        verify(entry, payload, javaHome);
        return executable(payload, entry);
    }

    private Path cachedArchive(ExternalToolCatalog.Entry entry) throws IOException {
        Path root = layout.cacheRoot().resolve("downloads");
        Files.createDirectories(root);
        return root.resolve(entry.assetName());
    }

    private Path payload(Path target, ExternalToolCatalog.Entry entry) throws IOException {
        if (entry.name().equals("bundletool")) return target.resolve(entry.payload());
        if (!Files.isDirectory(target)) return target.resolve(entry.payload());
        try (var paths = Files.walk(target)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals("protoc")
                            || path.getFileName().toString().equals("protoc.exe"))
                    .findFirst().orElse(target.resolve(entry.payload()));
        }
    }

    private void prepareExecutable(Path tool) throws IOException {
        if (layout.platform().operatingSystem() != HostPlatform.OperatingSystem.WINDOWS) {
            Files.setPosixFilePermissions(tool, java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x"));
        }
        if (layout.platform().operatingSystem() == HostPlatform.OperatingSystem.MACOS) clearMacQuarantine(tool);
    }

    private void clearMacQuarantine(Path tool) throws IOException {
        ProcessResult query;
        try {
            query = processes.run(new ProcessRequest(List.of("/usr/bin/xattr", "-p", "com.apple.quarantine",
                    tool.toString()), null, null, Duration.ofSeconds(10)));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while checking macOS quarantine for " + tool, interrupted);
        }
        if (query.exitCode() == 1) return; // xattr reports an absent optional attribute this way.
        if (!query.succeeded()) throw new IOException("Could not inspect macOS quarantine for " + tool);
        try {
            ProcessResult remove = processes.run(new ProcessRequest(List.of("/usr/bin/xattr", "-d",
                    "com.apple.quarantine", tool.toString()), null, null, Duration.ofSeconds(10)));
            if (!remove.succeeded()) throw new IOException("Could not remove macOS quarantine from " + tool);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while removing macOS quarantine from " + tool, interrupted);
        }
    }

    private void verify(ExternalToolCatalog.Entry entry, Path tool, Path javaHome) throws IOException {
        List<String> command = entry.name().equals("bundletool")
                ? List.of(javaExecutable(javaHome).toString(), "-jar", tool.toString(), "version")
                : List.of(tool.toString(), "--version");
        try {
            ProcessResult result = processes.run(new ProcessRequest(command, null, null, Duration.ofSeconds(30)));
            String output = (result.stdout() + " " + result.stderr()).trim();
            if (!result.succeeded() || output.isBlank()
                    || (!entry.name().equals("bundletool") && !output.contains("21.0"))) {
                throw new IOException("Version probe failed for " + entry.name() + ": " + output);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while probing " + entry.name(), interrupted);
        }
    }

    private Path executable(Path payload, ExternalToolCatalog.Entry entry) throws IOException {
        if (!Files.isRegularFile(payload)) throw new IOException("Installed tool payload is missing: " + payload);
        return payload;
    }

    private static Path javaExecutable(Path javaHome) {
        if (javaHome == null) return Path.of("java");
        String suffix = System.getProperty("os.name", "").toLowerCase().startsWith("windows") ? ".exe" : "";
        Path candidate = javaHome.resolve("bin").resolve("java" + suffix);
        return Files.isRegularFile(candidate) ? candidate : Path.of("java");
    }
}
