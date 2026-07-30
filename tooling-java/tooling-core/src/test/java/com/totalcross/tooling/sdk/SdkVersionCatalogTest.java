// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.sdk;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SdkVersionCatalogTest {
  @TempDir Path directory;
  private static final String METADATA = "<metadata><versioning><versions><version>7.3.0</version><version>7.6.0</version><version>7.7.0-RC1</version></versions></versioning></metadata>";

  @Test void selects_the_highest_stable_version_and_reuses_a_verified_cache() throws Exception {
    Path cache = directory.resolve("maven-metadata.xml");
    assertEquals("7.6.0", new SdkVersionCatalog(cache, () -> METADATA).latestStable());
    assertEquals("7.6.0", new SdkVersionCatalog(cache, () -> { throw new IOException("offline"); }).latestStable());
  }

  @Test void rejects_a_tampered_cached_metadata_file() throws Exception {
    Path cache = directory.resolve("maven-metadata.xml");
    new SdkVersionCatalog(cache, () -> METADATA).latestStable();
    java.nio.file.Files.writeString(cache, "<metadata><version>9.9.9</version></metadata>");
    assertThrows(IOException.class, () -> new SdkVersionCatalog(cache, () -> { throw new IOException("offline"); }).latestStable());
  }
}
