// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.build;

import java.nio.file.Path;
import java.util.List;

public record ProjectModel(BuildTool buildTool, Path project, List<SourceRoot> sourceRoots,
    List<SourceRoot> testSourceRoots, List<ResourceRoot> resourceRoots, ClassOutput classOutput,
    ClassOutput testClassOutput, DependencyClasspath dependencies, String mainClass, String sdkVersion,
    String sdkCoordinate, String toolingJdkIdentity, JavaCompatibilityPolicy javaPolicy,
    RetrolambdaPlan retrolambda, List<String> launcherArguments, List<String> deployArguments,
    List<String> platforms, PreviewSessionDescriptor preview) {
  public static final int SCHEMA_VERSION = 1;

  public ProjectModel {
    if (buildTool == null || project == null || classOutput == null || testClassOutput == null || dependencies == null
        || javaPolicy == null || retrolambda == null || preview == null) throw new IllegalArgumentException("invalid project model");
    project = project.toAbsolutePath().normalize();
    sourceRoots = List.copyOf(sourceRoots);
    testSourceRoots = List.copyOf(testSourceRoots);
    resourceRoots = List.copyOf(resourceRoots);
    mainClass = mainClass == null ? "" : mainClass;
    sdkVersion = sdkVersion == null ? "" : sdkVersion;
    sdkCoordinate = sdkCoordinate == null ? "" : sdkCoordinate;
    toolingJdkIdentity = toolingJdkIdentity == null ? "" : toolingJdkIdentity;
    launcherArguments = List.copyOf(launcherArguments);
    deployArguments = List.copyOf(deployArguments);
    platforms = List.copyOf(platforms);
  }
}
