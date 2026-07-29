// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import static org.junit.jupiter.api.Assertions.*;
import com.totalcross.tooling.download.DownloadRequest;
import com.totalcross.tooling.download.Downloader;
import com.totalcross.tooling.platform.HostPlatform;
import com.totalcross.tooling.platform.StoreLayout;
import com.totalcross.tooling.store.ChecksumVerifier;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class JdkCatalogInstallerTest {
  @Test
  void installsProbesRecordsAndReusesAnImmutableCatalogJdkOffline() throws Exception {
    Path root = Files.createTempDirectory("jdk-catalog-install");
    Path archive = root.resolve("jdk.zip");
    zip(archive, "fake-jdk/bin/java", "#!/bin/sh\nexit 0\n");
    append(archive, "fake-jdk/bin/javac", "#!/bin/sh\nexit 0\n");
    String digest = ChecksumVerifier.sha256(archive);
    HostPlatform host = HostPlatform.detect();
    JdkCatalogEntry entry = entry(host, digest);
    StoreLayout layout = new StoreLayout(host, root.resolve("data"), root.resolve("cache"));
    AtomicInteger downloads = new AtomicInteger();
    Downloader downloader = request -> {
      downloads.incrementAndGet();
      Files.createDirectories(request.destination().getParent());
      Files.copy(archive, request.destination());
    };
    JdkCatalogInstaller online = new JdkCatalogInstaller(layout, downloader, new JdkCapabilityProbe(host), false);
    JdkInstallation installed = online.install(entry, null);
    assertTrue(Files.isRegularFile(installed.home().resolve("bin/java")));
    assertEquals(1, downloads.get());
    assertTrue(Files.readString(installed.home().getParent().resolve("jdk-catalog.properties")).contains("entryId=test-jdk"));
    JdkInstallation reused = new JdkCatalogInstaller(layout, request -> fail("offline reuse must not download"),
        new JdkCapabilityProbe(host), true).install(entry, null);
    assertEquals(installed.home(), reused.home());
  }

  @Test
  void unsupportedHostAndMissingOfflineInstallRequestJdkPath() throws Exception {
    HostPlatform host = HostPlatform.from("Linux", "amd64");
    Path root = Files.createTempDirectory("jdk-catalog-offline");
    StoreLayout layout = new StoreLayout(host, root.resolve("data"), root.resolve("cache"));
    JdkCatalogEntry other = entry(HostPlatform.from("Windows 11", "amd64"), "a".repeat(64));
    JdkCatalogInstaller installer = new JdkCatalogInstaller(layout, request -> fail("no download"), new JdkCapabilityProbe(host), true);
    assertTrue(assertThrows(JdkSelectionException.class, () -> installer.install(other, null)).getMessage().contains("jdkPath"));
    JdkCatalogEntry local = entry(host, "a".repeat(64));
    assertTrue(assertThrows(JdkSelectionException.class, () -> installer.install(local, null)).getMessage().contains("jdkPath"));
  }

  private static JdkCatalogEntry entry(HostPlatform host, String digest) {
    return new JdkCatalogEntry(1, "test-jdk", "Test", 17, "17.0.0", "1", host, JdkArchiveType.ZIP,
        URI.create("https://example.test/jdk.zip"), digest, "fake-jdk", null, null);
  }

  private static void zip(Path archive, String name, String content) throws IOException {
    try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
      output.putNextEntry(new ZipEntry(name));
      output.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      output.closeEntry();
    }
  }

  private static void append(Path archive, String name, String content) throws IOException {
    Path replacement = archive.resolveSibling("replacement.zip");
    try (var input = new java.util.zip.ZipInputStream(Files.newInputStream(archive));
         ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(replacement))) {
      ZipEntry existing;
      while ((existing = input.getNextEntry()) != null) {
        output.putNextEntry(new ZipEntry(existing.getName()));
        input.transferTo(output);
        output.closeEntry();
      }
      output.putNextEntry(new ZipEntry(name));
      output.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      output.closeEntry();
    }
    Files.move(replacement, archive, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
  }
}
