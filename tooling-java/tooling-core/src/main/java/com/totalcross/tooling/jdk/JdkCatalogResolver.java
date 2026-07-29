// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import com.totalcross.tooling.download.HttpDownloader;
import com.totalcross.tooling.platform.HostPlatform;
import com.totalcross.tooling.platform.StoreLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Resolves a probed explicit JDK first, then reviewed catalog entries for the requested major. */
public final class JdkCatalogResolver {
  private final JdkCatalogSource source;
  private final JdkCatalogInstaller installer;
  private final JdkSelector selector;
  private final HostPlatform host;

  public static JdkCatalogResolver production() {
    HostPlatform host = HostPlatform.detect();
    JdkCapabilityProbe probe = new JdkCapabilityProbe(host);
    return new JdkCatalogResolver(new BundledJdkCatalogSource(),
        new JdkCatalogInstaller(StoreLayout.detect(), new HttpDownloader(), probe, false),
        new JdkSelector(probe), host);
  }

  public JdkCatalogResolver(JdkCatalogSource source, JdkCatalogInstaller installer, JdkSelector selector,
      HostPlatform host) {
    this.source = source;
    this.installer = installer;
    this.selector = selector;
    this.host = host;
  }

  public JdkInstallation resolve(JdkRequest request) throws JdkSelectionException {
    if (request.explicitPath() != null) return selector.select(request, List.of());
    int major = major(request.version());
    List<JdkCatalogEntry> candidates;
    try {
      candidates = new JdkCatalogParser().parse(source).candidates(host, major);
    } catch (IOException | IllegalArgumentException error) {
      throw new JdkSelectionException(List.of("Unable to load the JDK catalog: " + error.getMessage()));
    }
    if (candidates.isEmpty()) throw new JdkSelectionException(List.of(
        "No catalog JDK " + major + " entry supports " + host.id() + "; configure jdkPath"));
    List<String> diagnostics = new ArrayList<>();
    for (JdkCatalogEntry candidate : candidates) {
      try {
        return installer.install(candidate, request.protoc());
      } catch (IOException | JdkSelectionException error) {
        diagnostics.add(candidate.entryId() + ": " + error.getMessage());
      }
    }
    diagnostics.add("Configure jdkPath to use a compatible local JDK");
    throw new JdkSelectionException(diagnostics);
  }

  private static int major(String value) throws JdkSelectionException {
    try { return Integer.parseInt(value); }
    catch (NumberFormatException error) { throw new JdkSelectionException(List.of("Invalid requested JDK major: " + value)); }
  }
}
