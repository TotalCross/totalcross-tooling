// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import com.totalcross.tooling.platform.HostPlatform;
import java.net.URI;
import java.nio.file.Path;

/** One reviewed, immutable JDK archive that may be installed on exactly one host. */
public record JdkCatalogEntry(int schemaVersion, String entryId, String vendor, int javaMajor,
    String version, String build, HostPlatform platform, JdkArchiveType archiveType, URI url,
    String sha256, String javaHomeRelativePath, Long archiveSize, String releaseDate) {
  public JdkCatalogEntry {
    if (schemaVersion != JdkCatalog.SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported JDK catalog schema: " + schemaVersion);
    require(entryId, "entryId");
    require(vendor, "vendor");
    require(version, "version");
    require(build, "build");
    if (javaMajor < 8 || platform == null || archiveType == null || url == null) throw new IllegalArgumentException("JDK catalog entry is incomplete");
    if (!"https".equalsIgnoreCase(url.getScheme()) || url.toString().toLowerCase().contains("latest")) throw new IllegalArgumentException("JDK URL must be concrete HTTPS: " + url);
    if (sha256 == null || !sha256.matches("[0-9a-f]{64}") || sha256.chars().allMatch(value -> value == '0')) throw new IllegalArgumentException("JDK SHA-256 must be a concrete digest");
    Path relative = Path.of(javaHomeRelativePath == null ? "" : javaHomeRelativePath).normalize();
    if (relative.isAbsolute() || relative.startsWith("..") || relative.toString().isBlank()) throw new IllegalArgumentException("javaHomeRelativePath must stay inside the archive");
    if (archiveSize != null && archiveSize <= 0) throw new IllegalArgumentException("archiveSize must be positive when declared");
  }

  public Path javaHome(Path installationRoot) {
    Path home = installationRoot.resolve(javaHomeRelativePath).normalize();
    if (!home.startsWith(installationRoot.toAbsolutePath().normalize())) throw new IllegalArgumentException("JDK home escapes installation root");
    return home;
  }

  private static void require(String value, String field) {
    if (value == null || value.isBlank() || value.contains("/") || value.contains("\\") || value.equals("..")) throw new IllegalArgumentException(field + " must be one safe path component");
  }
}
