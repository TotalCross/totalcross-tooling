// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.store;

import com.totalcross.tooling.platform.StoreLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;

/** Installs one immutable artifact through staging, verification, and atomic promotion. */
public final class ArtifactInstaller {
    private static final String COMPLETE = ".totalcross-install-complete";
    private final StoreLayout layout;
    private final ArchiveExtractor extractor;
    private final FileLockManager locks;

    public ArtifactInstaller(StoreLayout layout) {
        this(layout, new ArchiveExtractor(), new FileLockManager());
    }

    ArtifactInstaller(StoreLayout layout, ArchiveExtractor extractor, FileLockManager locks) {
        this.layout = layout;
        this.extractor = extractor;
        this.locks = locks;
    }

    public InstalledArtifact install(InstallRequest request) throws IOException {
        Path target = layout.installationRoot(request.coordinate().kind(), request.coordinate().id());
        Path lockPath = target.resolveSibling(target.getFileName() + ".lock");
        try (FileLockManager.Lock ignored = locks.acquire(lockPath)) {
            if (Files.isRegularFile(target.resolve(COMPLETE))) {
                return new InstalledArtifact(request.coordinate(), target, request.sha256());
            }
            if (Files.exists(target)) throw new IOException("Incomplete installation exists: " + target);
            ChecksumVerifier.verify(request.archive(), request.sha256());
            Path stagingParent = layout.stagingRoot();
            Files.createDirectories(stagingParent);
            Path staging = stagingParent.resolve(request.coordinate().id() + "-" + UUID.randomUUID());
            try {
                extractor.extract(request.archive(), staging);
                writeMetadata(staging, request);
                Files.createDirectories(target.getParent());
                moveAtomically(staging, target);
                return new InstalledArtifact(request.coordinate(), target, request.sha256());
            } finally {
                if (Files.exists(staging)) deleteTree(staging);
            }
        }
    }

    private static void writeMetadata(Path root, InstallRequest request) throws IOException {
        Properties metadata = new Properties();
        metadata.setProperty("kind", request.coordinate().kind());
        metadata.setProperty("name", request.coordinate().name());
        metadata.setProperty("version", request.coordinate().version());
        metadata.setProperty("build", request.coordinate().build());
        metadata.setProperty("sha256", request.sha256());
        metadata.setProperty("source", request.source().toString());
        metadata.setProperty("host", request.hostPlatform().id());
        try (var output = Files.newOutputStream(root.resolve("metadata.properties"))) {
            metadata.store(output, "TotalCross immutable installation");
        }
        Files.writeString(root.resolve(COMPLETE), "complete\n");
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFailure) {
            Files.move(source, target);
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
