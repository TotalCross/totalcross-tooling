// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling;

import com.totalcross.tooling.download.DownloadRequest;
import com.totalcross.tooling.download.Downloader;
import com.totalcross.tooling.platform.HostPlatform;
import com.totalcross.tooling.platform.StoreLayout;
import com.totalcross.tooling.store.ChecksumVerifier;
import com.totalcross.tooling.store.ExternalToolCatalog;
import com.totalcross.tooling.store.ExternalToolResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExternalToolResolverTest {
    @Test
    void concreteCatalogHasAllHostEntriesAndNoPlaceholderChecksum() {
        assertTrue(ExternalToolCatalog.entries().values().stream()
                .allMatch(entry -> entry.sha256().matches("[0-9a-f]{64}") && !entry.source().toString().contains("latest")));
        assertNotNull(ExternalToolCatalog.entry("protoc", HostPlatform.from("Linux", "amd64")));
        assertNotNull(ExternalToolCatalog.entry("protoc", HostPlatform.from("Mac OS X", "aarch64")));
        assertNotNull(ExternalToolCatalog.entry("protoc", HostPlatform.from("Windows 11", "arm64")));
        assertNull(ExternalToolCatalog.entry("missing", HostPlatform.detect()));
    }

    @Test
    void installsVerifiesReusesOfflineAndSerializesConcurrentRequests() throws Exception {
        Path root = Files.createTempDirectory("external-tool-test");
        Path archive = root.resolve("protoc-test.zip");
        zip(archive, "protoc-test/bin/protoc", "#!/bin/sh\necho libprotoc 21.0\n");
        String digest = ChecksumVerifier.sha256(archive);
        ExternalToolCatalog.Entry entry = new ExternalToolCatalog.Entry("protoc", "21.0", "test-host",
                "protoc-test.zip", "bin/protoc", digest, URI.create("https://example.test/protoc-test.zip"));
        StoreLayout layout = new StoreLayout(HostPlatform.detect(), root.resolve("data"), root.resolve("cache"));
        CountingDownloader downloader = new CountingDownloader(archive);
        ExternalToolResolver resolver = new ExternalToolResolver(layout, downloader, false);
        Path installed = resolver.resolve(entry, Path.of(System.getProperty("java.home")));
        assertTrue(Files.isExecutable(installed));
        assertEquals(installed, new ExternalToolResolver(layout, (request) -> {
            throw new IOException("network should not be used offline");
        }, true).resolve(entry, Path.of(System.getProperty("java.home"))));

        Path concurrentRoot = Files.createTempDirectory("external-tool-concurrent");
        StoreLayout concurrentLayout = new StoreLayout(HostPlatform.detect(), concurrentRoot.resolve("data"), concurrentRoot.resolve("cache"));
        CountingDownloader concurrentDownloader = new CountingDownloader(archive);
        ExternalToolResolver concurrent = new ExternalToolResolver(concurrentLayout, concurrentDownloader, false);
        var executor = Executors.newFixedThreadPool(2);
        var first = executor.submit(() -> concurrent.resolve(entry, Path.of(System.getProperty("java.home"))));
        var second = executor.submit(() -> concurrent.resolve(entry, Path.of(System.getProperty("java.home"))));
        assertEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        assertEquals(1, concurrentDownloader.downloads.get());
        executor.shutdownNow();
    }

    @Test
    void rejectsChecksumMismatchWithoutCompletingInstallation() throws Exception {
        Path root = Files.createTempDirectory("external-tool-checksum");
        Path archive = root.resolve("protoc-test.zip");
        zip(archive, "protoc-test/bin/protoc", "#!/bin/sh\necho libprotoc 21.0\n");
        ExternalToolCatalog.Entry entry = new ExternalToolCatalog.Entry("protoc", "21.0", "bad-checksum",
                "protoc-bad.zip", "bin/protoc", "0".repeat(64), URI.create("https://example.test/protoc-bad.zip"));
        StoreLayout layout = new StoreLayout(HostPlatform.detect(), root.resolve("data"), root.resolve("cache"));
        assertThrows(IOException.class, () -> new ExternalToolResolver(layout, new CountingDownloader(archive), false)
                .resolve(entry, Path.of(System.getProperty("java.home"))));
        assertFalse(Files.exists(layout.externalToolRoot("protoc", "21.0", "bad-checksum")
                .resolve(".totalcross-install-complete")));
    }

    @Test
    void rejectsACompletedArchiveWhenItsVersionProbeFails() throws Exception {
        Path root = Files.createTempDirectory("external-tool-probe");
        Path archive = root.resolve("protoc-wrong.zip");
        zip(archive, "protoc-wrong/bin/protoc", "#!/bin/sh\necho libprotoc 99.0\n");
        ExternalToolCatalog.Entry entry = new ExternalToolCatalog.Entry("protoc", "21.0", "bad-probe",
                "protoc-wrong.zip", "bin/protoc", ChecksumVerifier.sha256(archive),
                URI.create("https://example.test/protoc-wrong.zip"));
        StoreLayout layout = new StoreLayout(HostPlatform.detect(), root.resolve("data"), root.resolve("cache"));
        assertThrows(IOException.class, () -> new ExternalToolResolver(layout, new CountingDownloader(archive), false)
                .resolve(entry, Path.of(System.getProperty("java.home"))));
    }

    private static void zip(Path path, String entry, String content) throws IOException {
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(path))) {
            output.putNextEntry(new ZipEntry(entry));
            output.write(content.getBytes());
            output.closeEntry();
        }
    }

    private static final class CountingDownloader implements Downloader {
        private final Path source;
        private final AtomicInteger downloads = new AtomicInteger();

        private CountingDownloader(Path source) {
            this.source = source;
        }

        @Override
        public void download(DownloadRequest request) throws IOException {
            downloads.incrementAndGet();
            Files.createDirectories(request.destination().getParent());
            Files.copy(source, request.destination(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
