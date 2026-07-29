// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import static org.junit.jupiter.api.Assertions.*;
import com.totalcross.tooling.platform.HostPlatform;
import com.totalcross.tooling.platform.StoreLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class JdkCatalogResolverTest {
  @Test
  void explicitJdkPathWinsBeforeTheCatalogAndIsCapabilityProbed() throws Exception {
    HostPlatform host = HostPlatform.detect();
    JdkCapabilityProbe probe = new JdkCapabilityProbe(host);
    JdkCatalogResolver resolver = new JdkCatalogResolver(new InMemoryJdkCatalogSource(catalog(host)),
        new JdkCatalogInstaller(layout(host), request -> fail("catalog must not download"), probe, true),
        new JdkSelector(probe), host);
    Path home = Path.of(System.getProperty("java.home"));
    assertEquals(home, resolver.resolve(new JdkRequest("17", home, null)).home());
  }

  @Test
  void unavailableCatalogEntryReturnsAnActionableJdkPathDiagnostic() throws Exception {
    HostPlatform host = HostPlatform.detect();
    JdkCapabilityProbe probe = new JdkCapabilityProbe(host);
    JdkCatalogResolver resolver = new JdkCatalogResolver(new InMemoryJdkCatalogSource(catalog(host)),
        new JdkCatalogInstaller(layout(host), request -> fail("offline resolution must not download"), probe, true),
        new JdkSelector(probe), host);
    assertTrue(assertThrows(JdkSelectionException.class, () -> resolver.resolve(new JdkRequest("17", null, null)))
        .getMessage().contains("jdkPath"));
  }

  private static StoreLayout layout(HostPlatform host) throws Exception {
    Path root = Files.createTempDirectory("catalog-resolver");
    return new StoreLayout(host, root.resolve("data"), root.resolve("cache"));
  }

  private static String catalog(HostPlatform host) {
    String os = switch (host.operatingSystem()) { case MACOS -> "Mac OS X"; case WINDOWS -> "Windows 11"; case LINUX -> "Linux"; };
    String arch = host.architecture() == HostPlatform.Architecture.ARM64 ? "aarch64" : "amd64";
    return "schemaVersion=1\nentry.test.vendor=Test\nentry.test.javaMajor=17\nentry.test.version=17.0.0\n"
        + "entry.test.build=1\nentry.test.operatingSystem=" + os + "\nentry.test.architecture=" + arch + "\n"
        + "entry.test.archiveType=zip\nentry.test.url=https\\://example.test/jdk.zip\nentry.test.sha256=" + "a".repeat(64) + "\n"
        + "entry.test.javaHomeRelativePath=fake-jdk\n";
  }
}
