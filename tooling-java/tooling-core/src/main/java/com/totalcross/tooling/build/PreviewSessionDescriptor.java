// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.build;

import java.nio.file.Path;

public record PreviewSessionDescriptor(int version, BuildTool buildTool, Path project, Path descriptor,
    String mainClass, String sdkVersion) {
  public PreviewSessionDescriptor {
    if (version != 1 || buildTool == null || project == null || descriptor == null) throw new IllegalArgumentException("invalid session");
    project = project.toAbsolutePath().normalize();
    descriptor = descriptor.toAbsolutePath().normalize();
  }

  public String toJson() {
    return "{\"version\":" + version + ",\"buildTool\":\"" + buildTool + "\",\"project\":\""
        + escape(project.toString()) + "\",\"descriptor\":\"" + escape(descriptor.toString())
        + "\",\"mainClass\":\"" + escape(mainClass == null ? "" : mainClass) + "\",\"sdkVersion\":\""
        + escape(sdkVersion == null ? "" : sdkVersion) + "\"}";
  }

  private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
