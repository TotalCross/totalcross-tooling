// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import com.totalcross.tooling.download.DownloadRequest;
import com.totalcross.tooling.download.Downloader;
import com.totalcross.tooling.platform.HostPlatform;
import com.totalcross.tooling.platform.StoreLayout;
import com.totalcross.tooling.store.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Installs one catalog JDK atomically, verifies it, then records its reviewed identity. */
public final class JdkCatalogInstaller {
  private final StoreLayout layout;
  private final Downloader downloader;
  private final ArtifactInstaller installer;
  private final JdkCapabilityProbe probe;
  private final boolean offline;

  public JdkCatalogInstaller(StoreLayout layout, Downloader downloader, JdkCapabilityProbe probe, boolean offline) {
    this(layout, downloader, new ArtifactInstaller(layout), probe, offline);
  }

  JdkCatalogInstaller(StoreLayout layout, Downloader downloader, ArtifactInstaller installer, JdkCapabilityProbe probe, boolean offline) {
    this.layout = layout;
    this.downloader = downloader;
    this.installer = installer;
    this.probe = probe;
    this.offline = offline;
  }

  public JdkInstallation install(JdkCatalogEntry entry, Path protoc) throws IOException, JdkSelectionException {
    if (!layout.platform().equals(entry.platform())) throw new JdkSelectionException(java.util.List.of(
        "JDK catalog entry " + entry.entryId() + " does not support " + layout.platform().id() + "; configure jdkPath"));
    ArtifactCoordinate coordinate = new ArtifactCoordinate("jdk", entry.entryId(), entry.version(), entry.build());
    Path target = layout.installationRoot(coordinate.kind(), coordinate.id());
    if (!Files.isRegularFile(target.resolve(".totalcross-install-complete"))) installArchive(entry, coordinate, protoc);
    JdkInstallation installation = installation(entry, target);
    JdkCapabilityReport result = probe.probe(installation, protoc);
    if (!result.accepted()) throw new JdkSelectionException(result.failures());
    return installation;
  }

  private void installArchive(JdkCatalogEntry entry, ArtifactCoordinate coordinate, Path protoc) throws IOException, JdkSelectionException {
    Path archive = archivePath(entry);
    if (!Files.isRegularFile(archive)) {
      if (offline) throw new JdkSelectionException(java.util.List.of("JDK " + entry.entryId() + " is not installed for offline reuse; configure jdkPath"));
      downloader.download(new DownloadRequest(entry.url(), archive, entry.sha256()));
    } else {
      ChecksumVerifier.verify(archive, entry.sha256());
    }
    try {
      installer.install(new InstallRequest(coordinate, archive, entry.url(), entry.sha256(), layout.platform()), staging -> {
        JdkInstallation installation = installation(entry, staging);
        makeExecutable(installation.home());
        JdkCapabilityReport result = probe.probe(installation, protoc);
        if (!result.accepted()) throw new IOException("JDK capability probe failed: " + String.join(", ", result.failures()));
        writeCatalogMetadata(staging, entry);
      });
    } catch (IOException error) {
      if (error.getMessage() != null && error.getMessage().startsWith("JDK capability probe failed:")) {
        throw new JdkSelectionException(java.util.List.of(error.getMessage()));
      }
      throw error;
    }
  }

  private Path archivePath(JdkCatalogEntry entry) {
    String extension = entry.archiveType() == JdkArchiveType.ZIP ? ".zip" : ".tar.gz";
    return layout.cacheRoot().resolve("downloads/jdk").resolve(entry.entryId() + "-" + entry.sha256() + extension);
  }

  private static JdkInstallation installation(JdkCatalogEntry entry, Path root) throws IOException {
    Path home = entry.javaHome(root);
    if (!Files.isDirectory(home)) throw new IOException("JDK archive does not contain declared JAVA_HOME: " + entry.javaHomeRelativePath());
    return new JdkInstallation(entry.vendor(), entry.version(), entry.build(), home);
  }

  private static void makeExecutable(Path home) throws IOException {
    if (Files.getFileAttributeView(home, java.nio.file.attribute.PosixFileAttributeView.class) == null) return;
    for (String executable : java.util.List.of("java", "javac")) {
      Path path = home.resolve("bin").resolve(executable);
      if (Files.isRegularFile(path)) {
        Files.setPosixFilePermissions(path, java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x"));
      }
    }
  }

  private static void writeCatalogMetadata(Path root, JdkCatalogEntry entry) throws IOException {
    Properties properties = new Properties();
    properties.setProperty("schemaVersion", String.valueOf(entry.schemaVersion()));
    properties.setProperty("entryId", entry.entryId());
    properties.setProperty("vendor", entry.vendor());
    properties.setProperty("url", entry.url().toString());
    properties.setProperty("sha256", entry.sha256());
    properties.setProperty("javaHomeRelativePath", entry.javaHomeRelativePath());
    try (var output = Files.newOutputStream(root.resolve("jdk-catalog.properties"))) {
      properties.store(output, "TotalCross immutable JDK catalog entry");
    }
  }
}
