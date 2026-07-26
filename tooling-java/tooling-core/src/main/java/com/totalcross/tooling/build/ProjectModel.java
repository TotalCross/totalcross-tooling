// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.build;

import java.nio.file.Path;
import java.util.List;

public record ProjectModel(BuildTool buildTool, Path project, List<SourceRoot> sourceRoots,
    List<ResourceRoot> resourceRoots, ClassOutput classOutput, DependencyClasspath dependencies,
    JavaCompatibilityPolicy javaPolicy, RetrolambdaPlan retrolambda, PreviewSessionDescriptor preview) {
  public ProjectModel {
    project = project.toAbsolutePath().normalize();
    sourceRoots = List.copyOf(sourceRoots);
    resourceRoots = List.copyOf(resourceRoots);
  }
}
