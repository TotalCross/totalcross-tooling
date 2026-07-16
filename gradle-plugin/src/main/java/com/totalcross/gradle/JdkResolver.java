/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.gradle;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves a runnable Zulu JDK 17 for tc.Deploy. */
public final class JdkResolver {
    static final String JDK_FOLDER = "zulu_jdk_17";
    private final String jdkVersion;
    private final Path cache;
    private final ArchiveDownloader downloader;
    private final String osName;
    private final String architecture;

    public JdkResolver(Path gradleUserHome, ArchiveDownloader downloader) {
        this(gradleUserHome, downloader, "17", System.getProperty("os.name"), System.getProperty("os.arch"));
    }

    JdkResolver(Path gradleUserHome, ArchiveDownloader downloader, String osName, String architecture) {
        this(gradleUserHome, downloader, "17", osName, architecture);
    }

    JdkResolver(Path gradleUserHome, ArchiveDownloader downloader, String jdkVersion) {
        this(gradleUserHome, downloader, jdkVersion, System.getProperty("os.name"), System.getProperty("os.arch"));
    }

    private JdkResolver(Path gradleUserHome, ArchiveDownloader downloader, String jdkVersion, String osName, String architecture) {
        this.jdkVersion = jdkVersion;
        this.cache = gradleUserHome.resolve("caches/totalcross/jdk").resolve("zulu_jdk_" + jdkVersion);
        this.downloader = downloader;
        this.osName = osName;
        this.architecture = architecture;
    }

    public File resolve(File configuredHome) throws IOException {
        if (configuredHome != null) return validate(configuredHome.toPath()).toFile();
        try (CacheLock ignored = CacheLock.acquire(cache.getParent())) {
            return resolveFromCache();
        }
    }

    private File resolveFromCache() throws IOException {
        Path cachedHome = cacheJavaHome();
        if (Files.isRegularFile(javaExecutable(cachedHome))) {
            makeBinariesExecutable(cachedHome);
            return validate(cachedHome).toFile();
        }
        Path parent = cache.getParent();
        String cacheName = cache.getFileName().toString();
        Path archive = parent.resolve(cacheName + ".zip");
        Path staging = parent.resolve(cacheName + ".extracting");
        SdkResolver.deleteTree(staging);
        downloader.download(downloadUri(), archive);
        downloader.extract(archive, staging);
        Path javaHome;
        try (var paths = Files.walk(staging)) {
            javaHome = paths.filter(path -> Files.isRegularFile(javaExecutable(path))).findFirst()
                    .orElseThrow(() -> new IOException("The downloaded Zulu archive does not contain bin/java"));
        }
        Path cacheSource = isMac() ? javaHome.getParent().getParent() : javaHome;
        SdkResolver.deleteTree(cache);
        SdkResolver.move(cacheSource, cache);
        SdkResolver.deleteTree(staging);
        Files.deleteIfExists(archive);
        cachedHome = cacheJavaHome();
        makeBinariesExecutable(cachedHome);
        return validate(cachedHome).toFile();
    }

    URI downloadUri() {
        return URI.create("https://api.azul.com/zulu/download/community/v1.0/bundles/latest/binary/?jdk_version=" + jdkVersion + "&ext=zip&os="
                + azulOs() + "&arch=" + azulArchitecture() + "&hw_bitness=64&crac_supported=false");
    }

    private String azulOs() {
        String normalized = osName.toLowerCase();
        if (normalized.startsWith("windows")) return "windows";
        if (normalized.startsWith("mac")) return "macos";
        if (normalized.startsWith("linux")) return "linux";
        throw new IllegalStateException("Unsupported operating system for Zulu JDK: " + osName);
    }

    private String azulArchitecture() {
        String normalized = architecture.toLowerCase();
        return normalized.contains("aarch64") || normalized.contains("arm64") ? "arm" : "x86";
    }

    private Path validate(Path home) throws IOException {
        Path java = javaExecutable(home);
        if (!Files.isRegularFile(java) || !Files.isExecutable(java)) {
            throw new IOException("JDK home must contain an executable " + java + ": " + home);
        }
        return home;
    }

    private static void makeBinariesExecutable(Path javaHome) throws IOException {
        Path bin = javaHome.resolve("bin");
        if (!Files.isDirectory(bin)) return;
        try (var paths = Files.walk(bin)) {
            paths.filter(Files::isRegularFile).forEach(path -> path.toFile().setExecutable(true, false));
        }
    }

    private Path javaExecutable(Path home) {
        return home.resolve("bin").resolve(osName.toLowerCase().startsWith("windows") ? "java.exe" : "java");
    }

    private Path cacheJavaHome() {
        return isMac() ? cache.resolve("Contents/Home") : cache;
    }

    private boolean isMac() {
        return osName.toLowerCase().startsWith("mac");
    }
}
