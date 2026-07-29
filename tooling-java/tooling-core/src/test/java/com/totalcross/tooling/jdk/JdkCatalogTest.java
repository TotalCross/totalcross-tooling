// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class JdkCatalogTest {
  @Test
  void bundledCatalogCoversTheMinimumReleaseHostsWithConcreteEntries() throws Exception {
    JdkCatalog catalog = new JdkCatalogParser().parse(new BundledJdkCatalogSource());
    assertEquals(JdkCatalog.SCHEMA_VERSION, catalog.schemaVersion());
    assertEquals(8, catalog.entries().size());
    assertTrue(catalog.entries().stream().allMatch(entry -> (entry.javaMajor() == 11 || entry.javaMajor() == 17)
        && entry.url().getScheme().equals("https") && !entry.url().toString().contains("latest")
        && entry.sha256().matches("[0-9a-f]{64}")));
    assertEquals(1, catalog.candidates(com.totalcross.tooling.platform.HostPlatform.from("Mac OS X", "aarch64"), 17).size());
    assertEquals(1, catalog.candidates(com.totalcross.tooling.platform.HostPlatform.from("Mac OS X", "amd64"), 17).size());
    assertEquals(1, catalog.candidates(com.totalcross.tooling.platform.HostPlatform.from("Linux", "amd64"), 17).size());
    assertEquals(1, catalog.candidates(com.totalcross.tooling.platform.HostPlatform.from("Windows 11", "amd64"), 17).size());
    assertEquals(1, catalog.candidates(com.totalcross.tooling.platform.HostPlatform.from("Mac OS X", "aarch64"), 11).size());
    assertEquals(1, catalog.candidates(com.totalcross.tooling.platform.HostPlatform.from("Mac OS X", "amd64"), 11).size());
    assertEquals(1, catalog.candidates(com.totalcross.tooling.platform.HostPlatform.from("Linux", "amd64"), 11).size());
    assertEquals(1, catalog.candidates(com.totalcross.tooling.platform.HostPlatform.from("Windows 11", "amd64"), 11).size());
  }

  @Test
  void rejectsFloatingUrlsPlaceholderDigestsAndEscapingHomes() {
    assertThrows(IllegalArgumentException.class, () -> parse(entry("https\\://example.test/latest/jdk.zip", "0".repeat(64), "jdk")));
    assertThrows(IllegalArgumentException.class, () -> parse(entry("https\\://example.test/jdk.zip", "a".repeat(64), "../jdk")));
  }

  private static JdkCatalog parse(String value) {
    try {
      return new JdkCatalogParser().parse(new InMemoryJdkCatalogSource(value));
    } catch (java.io.IOException error) {
      throw new AssertionError(error);
    }
  }

  private static String entry(String url, String sha, String home) {
    return "schemaVersion=1\nentry.test.vendor=Test\nentry.test.javaMajor=17\nentry.test.version=17.0.0\n"
        + "entry.test.build=1\nentry.test.operatingSystem=Linux\nentry.test.architecture=amd64\n"
        + "entry.test.archiveType=zip\nentry.test.url=" + url + "\nentry.test.sha256=" + sha + "\n"
        + "entry.test.javaHomeRelativePath=" + home + "\n";
  }
}
