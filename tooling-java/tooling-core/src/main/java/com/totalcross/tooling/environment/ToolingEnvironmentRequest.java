// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.environment;

import com.totalcross.tooling.jdk.JdkCandidate;
import com.totalcross.tooling.jdk.JdkRequest;
import java.nio.file.Path;
import java.util.List;

/** Inputs shared by build adapters after they locate a candidate SDK installation. */
public record ToolingEnvironmentRequest(String sdkVersion, Path sdkHome, String sdkProvenance,
    int applicationJavaTarget, JdkRequest jdkRequest, List<JdkCandidate> jdkCandidates) {
  public ToolingEnvironmentRequest {
    if (sdkVersion == null || sdkVersion.isBlank()) throw new IllegalArgumentException("SDK version is required");
    if (sdkHome == null) throw new IllegalArgumentException("SDK home is required");
    if (sdkProvenance == null || sdkProvenance.isBlank()) throw new IllegalArgumentException("SDK provenance is required");
    if (jdkRequest == null) throw new IllegalArgumentException("tooling JDK request is required");
    jdkCandidates = List.copyOf(jdkCandidates == null ? List.of() : jdkCandidates);
  }
}
