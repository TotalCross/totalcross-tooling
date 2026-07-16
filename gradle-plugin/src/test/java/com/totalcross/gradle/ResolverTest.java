/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResolverTest {
    @TempDir Path temporaryDirectory;

    @Test
    void extractsSafeArchiveAndRejectsZipSlip() throws Exception {
        Path safe = temporaryDirectory.resolve("safe.zip");
        writeZip(safe, "sdk/etc/config.txt", "ok");
        Path destination = temporaryDirectory.resolve("extract");
        new ArchiveDownloader().extract(safe, destination);
        assertEquals("ok", Files.readString(destination.resolve("sdk/etc/config.txt")));

        Path unsafe = temporaryDirectory.resolve("unsafe.zip");
        writeZip(unsafe, "../outside.txt", "unsafe");
        assertThrows(IOException.class, () -> new ArchiveDownloader().extract(unsafe, destination));
        assertTrue(Files.notExists(temporaryDirectory.resolve("outside.txt")));
    }

    @Test
    void validatesConfiguredSdkHome() throws Exception {
        Path sdk = temporaryDirectory.resolve("sdk");
        Files.createDirectories(sdk.resolve("etc"));
        SdkResolver resolver = new SdkResolver(temporaryDirectory, new ArchiveDownloader());
        assertEquals(sdk.toFile(), resolver.resolve("7.2.2", sdk.toFile()));
        assertThrows(IOException.class, () -> resolver.resolve("7.2.2", temporaryDirectory.toFile()));
    }

    @Test
    void prefersGitHubReleaseArchiveWhenItExists() throws Exception {
        List<URI> downloads = new ArrayList<>();
        SdkReleaseLocator locator = version -> Optional.of(GitHubSdkReleaseLocator.githubArchiveUri(version));
        SdkResolver resolver = new SdkResolver(temporaryDirectory, new RecordingDownloader(downloads, false), locator);

        resolver.resolve("7.2.0", null);

        assertEquals(List.of(GitHubSdkReleaseLocator.githubArchiveUri("7.2.0")), downloads);
    }

    @Test
    void fallsBackToS3WhenGitHubReleaseDoesNotExist() throws Exception {
        List<URI> downloads = new ArrayList<>();
        SdkResolver resolver = new SdkResolver(temporaryDirectory, new RecordingDownloader(downloads, false), version -> Optional.empty());

        resolver.resolve("5.8.4", null);

        assertEquals(List.of(SdkResolver.s3ArchiveUri("5.8.4")), downloads);
    }

    @Test
    void fallsBackToS3WhenGitHubArchiveDownloadFails() throws Exception {
        List<URI> downloads = new ArrayList<>();
        SdkReleaseLocator locator = version -> Optional.of(GitHubSdkReleaseLocator.githubArchiveUri(version));
        SdkResolver resolver = new SdkResolver(temporaryDirectory, new RecordingDownloader(downloads, true), locator);

        resolver.resolve("7.2.0", null);

        assertEquals(List.of(GitHubSdkReleaseLocator.githubArchiveUri("7.2.0"), SdkResolver.s3ArchiveUri("7.2.0")), downloads);
    }

    @Test
    void doesNotLookUpOrDownloadWhenValidSdkIsCached() throws Exception {
        Path cachedSdk = temporaryDirectory.resolve("caches/totalcross/sdk/7.2.0/etc");
        Files.createDirectories(cachedSdk);
        AtomicInteger lookups = new AtomicInteger();
        List<URI> downloads = new ArrayList<>();
        SdkReleaseLocator locator = version -> {
            lookups.incrementAndGet();
            return Optional.of(GitHubSdkReleaseLocator.githubArchiveUri(version));
        };
        SdkResolver resolver = new SdkResolver(temporaryDirectory, new RecordingDownloader(downloads, false), locator);

        assertEquals(cachedSdk.getParent().toFile(), resolver.resolve("7.2.0", null));
        assertEquals(0, lookups.get());
        assertTrue(downloads.isEmpty());
    }

    @Test
    void buildsJdk17UrlsForSupportedPlatforms() {
        ArchiveDownloader downloader = new ArchiveDownloader();
        assertTrue(new JdkResolver(temporaryDirectory, downloader, "Mac OS X", "aarch64")
                .downloadUri().toString().contains("jdk_version=17"));
        assertTrue(new JdkResolver(temporaryDirectory, downloader, "Mac OS X", "aarch64")
                .downloadUri().toString().contains("os=macos&arch=arm"));
        assertTrue(new JdkResolver(temporaryDirectory, downloader, "Mac OS X", "aarch64")
                .downloadUri().toString().contains("crac_supported=false"));
        assertTrue(new JdkResolver(temporaryDirectory, downloader, "Linux", "amd64")
                .downloadUri().toString().contains("os=linux&arch=x86"));
        assertTrue(new JdkResolver(temporaryDirectory, downloader, "Windows 11", "amd64")
                .downloadUri().toString().contains("os=windows&arch=x86"));
    }

    @Test
    void preservesMacJdkBundleWhenCaching() throws Exception {
        ArchiveDownloader downloader = new ArchiveDownloader() {
            @Override
            public Path download(URI source, Path destination) throws IOException {
                writeZip(destination, "zulu-17.jdk/Contents/Home/bin/java", "java");
                return destination;
            }
        };
        JdkResolver resolver = new JdkResolver(temporaryDirectory, downloader, "Mac OS X", "aarch64");

        Path expectedHome = temporaryDirectory.resolve("caches/totalcross/jdk/zulu_jdk_17/Contents/Home");
        assertEquals(expectedHome.toFile(), resolver.resolve(null));
        assertTrue(Files.isExecutable(expectedHome.resolve("bin/java")));
    }

    private static void writeZip(Path target, String entryName, String content) throws IOException {
        Files.createDirectories(target.getParent());
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(target))) {
            output.putNextEntry(new ZipEntry(entryName));
            output.write(content.getBytes());
            output.closeEntry();
        }
    }

    private static final class RecordingDownloader extends ArchiveDownloader {
        private final List<URI> downloads;
        private final boolean failGitHub;

        private RecordingDownloader(List<URI> downloads, boolean failGitHub) {
            this.downloads = downloads;
            this.failGitHub = failGitHub;
        }

        @Override
        public Path download(URI source, Path destination) throws IOException {
            downloads.add(source);
            if (failGitHub && "github.com".equals(source.getHost())) {
                throw new IOException("simulated GitHub download failure");
            }
            writeZip(destination, "sdk/etc/config.txt", "ok");
            return destination;
        }
    }
}
