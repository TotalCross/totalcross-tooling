// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.environment;

import com.totalcross.tooling.compatibility.JavaCompatibilityPolicy;
import com.totalcross.tooling.jdk.*;
import java.nio.file.Files;
import java.nio.file.Path;

/** Applies shared SDK policy and probes the selected tooling JDK before use. */
public final class ToolingEnvironmentResolver {
  private final JdkSelector jdkSelector;

  public ToolingEnvironmentResolver(JdkSelector jdkSelector) { this.jdkSelector = jdkSelector; }

  public ToolingEnvironment resolve(ToolingEnvironmentRequest request) throws JdkSelectionException {
    Path sdkHome = request.sdkHome().toAbsolutePath().normalize();
    if (!Files.isDirectory(sdkHome.resolve("dist")) && !Files.isDirectory(sdkHome.resolve("etc"))) {
      throw new IllegalArgumentException("TotalCross SDK home must contain dist or etc: " + sdkHome);
    }
    JavaCompatibilityPolicy.validate(request.sdkVersion(), request.applicationJavaTarget());
    JdkInstallation jdk = jdkSelector.select(request.jdkRequest(), request.jdkCandidates());
    return new ToolingEnvironment(sdkHome, request.sdkVersion(), request.sdkProvenance(), jdk,
        request.applicationJavaTarget(), JavaCompatibilityPolicy.requiresRetrolambda(
            request.sdkVersion(), request.applicationJavaTarget()));
  }
}
