/*
 * SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.gradle;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Optional;

/** Resolves the full SDK distribution required by tc.Deploy. */
public final class SdkResolver {
    private final Path cacheRoot;
    private final ArchiveDownloader downloader;
    private final SdkReleaseLocator releaseLocator;

    public SdkResolver(Path gradleUserHome, ArchiveDownloader downloader) {
        this(gradleUserHome, downloader, new GitHubSdkReleaseLocator());
    }

    SdkResolver(Path gradleUserHome, ArchiveDownloader downloader, SdkReleaseLocator releaseLocator) {
        this.cacheRoot = gradleUserHome.resolve("caches/totalcross/sdk");
        this.downloader = downloader;
        this.releaseLocator = releaseLocator;
    }

    public File resolve(String version, File configuredHome) throws IOException {
        if (configuredHome != null) {
            return validate(configuredHome.toPath()).toFile();
        }
        try (CacheLock ignored = CacheLock.acquire(cacheRoot)) {
            return resolveFromCache(version);
        }
    }

    private File resolveFromCache(String version) throws IOException {
        Path cache = cacheRoot.resolve(version);
        if (hasSdkLayout(cache)) {
            createCompatibilityEtc(cache);
            return cache.toFile();
        }
        Path archive = cacheRoot.resolve("TotalCross-" + version + ".zip");
        Path staging = cacheRoot.resolve(version + ".extracting");
        deleteTree(staging);
        if (!Files.isRegularFile(archive)) {
            downloadArchive(version, archive);
        }
        downloader.extract(archive, staging);
        Path sdkRoot;
        try (var paths = Files.walk(staging)) {
            sdkRoot = paths.filter(SdkResolver::hasSdkLayout).findFirst()
                    .orElseThrow(() -> new IOException("The TotalCross SDK archive does not contain etc or dist"));
        }
        deleteTree(cache);
        move(sdkRoot, cache);
        deleteTree(staging);
        Files.deleteIfExists(archive);
        createCompatibilityEtc(cache);
        return validate(cache).toFile();
    }

    private void downloadArchive(String version, Path archive) throws IOException {
        URI fallback = s3ArchiveUri(version);
        Optional<URI> github = findGitHubArchive(version);
        if (github.isEmpty()) {
            downloader.download(fallback, archive);
            return;
        }
        try {
            downloader.download(github.get(), archive);
        } catch (IOException githubFailure) {
            Files.deleteIfExists(archive);
            try {
                downloader.download(fallback, archive);
            } catch (IOException fallbackFailure) {
                fallbackFailure.addSuppressed(githubFailure);
                throw fallbackFailure;
            }
        }
    }

    private Optional<URI> findGitHubArchive(String version) {
        try {
            return releaseLocator.findGitHubArchive(version);
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    static URI s3ArchiveUri(String version) {
        String prefix = version.length() >= 3 ? version.substring(0, 3) : version;
        return URI.create("https://totalcross-release.s3.amazonaws.com/" + prefix + "/TotalCross-" + version + ".zip");
    }

    private static Path validate(Path home) throws IOException {
        if (!hasSdkLayout(home)) {
            throw new IOException("TotalCross SDK home must contain etc or dist: " + home);
        }
        return home;
    }

    private static boolean hasSdkLayout(Path home) {
        return Files.isDirectory(home.resolve("etc")) || Files.isDirectory(home.resolve("dist"));
    }

    /** Recent SDK archives omit etc, although tc.Deploy still derives dist from it. */
    private static void createCompatibilityEtc(Path home) throws IOException {
        if (Files.isDirectory(home.resolve("etc")) || !Files.isDirectory(home.resolve("dist"))) return;
        Path fonts = home.resolve("etc/fonts");
        Files.createDirectories(fonts);
        Path materialIcons = home.resolve("dist/vm/Material Icons.tcz");
        if (Files.isRegularFile(materialIcons)) {
            Files.copy(materialIcons, fonts.resolve("Material Icons.tcz"), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static void deleteTree(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (var paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder()).forEach(candidate -> {
                try { Files.deleteIfExists(candidate); } catch (IOException error) { throw new DeleteFailure(error); }
            });
        } catch (DeleteFailure error) {
            throw error.getCause();
        }
    }

    private static final class DeleteFailure extends RuntimeException {
        DeleteFailure(IOException cause) { super(cause); }
        @Override public IOException getCause() { return (IOException) super.getCause(); }
    }
}
