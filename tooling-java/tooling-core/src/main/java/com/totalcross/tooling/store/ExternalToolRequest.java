// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.store;

import java.nio.file.Path;

public record ExternalToolRequest(String name, String version, String build, Path archive, String sha256,
    java.net.URI source, com.totalcross.tooling.platform.HostPlatform hostPlatform) {
  public ArtifactCoordinate coordinate() { return new ArtifactCoordinate("tools", name, version, build); }
  public InstallRequest installRequest() {
    return new InstallRequest(coordinate(), archive, source, sha256, hostPlatform);
  }
}
