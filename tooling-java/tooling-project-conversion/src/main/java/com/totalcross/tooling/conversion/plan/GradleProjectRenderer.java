/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.plan;

import com.totalcross.tooling.conversion.inference.JavaTargetInference;
import com.totalcross.tooling.compatibility.JavaCompatibilityPolicy;
import com.totalcross.tooling.sdk.SdkVersionCatalog;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Renders the minimal conventional Gradle files from a reviewed, unambiguous plan. */
public final class GradleProjectRenderer {
  @FunctionalInterface public interface SdkVersionResolver { String latestStable() throws IOException; }
  private final SdkVersionResolver sdkCatalog;

  public GradleProjectRenderer() { this(new SdkVersionCatalog()::latestStable); }
  GradleProjectRenderer(SdkVersionResolver sdkCatalog) { this.sdkCatalog = sdkCatalog; }

  public Map<Path, String> render(ConversionPlan plan, String pluginVersion) throws IOException {
    return render(plan, pluginVersion, null);
  }

  /** Renders an explicitly reviewed Java target instead of re-inferring it from legacy evidence. */
  public Map<Path, String> render(ConversionPlan plan, String pluginVersion, Integer selectedJavaTarget) throws IOException {
    if (plan.mainWindowCandidates().size() != 1) throw new IllegalArgumentException("select exactly one MainWindow before generating Gradle files");
    if (plan.sdkCandidates().size() > 1) throw new IllegalArgumentException("select exactly one SDK version before generating Gradle files");
    String sdk = plan.sdkCandidates().isEmpty() ? sdkCatalog.latestStable() : plan.sdkCandidates().get(0).version();
    int target = selectedJavaTarget == null ? new JavaTargetInference().infer(sdk, plan.scriptEvidence(), plan.project()).target() : selectedJavaTarget;
    JavaCompatibilityPolicy.validate(sdk, target);
    String mainClass = plan.mainWindowCandidates().get(0).className();
    String projectName = plan.project().getFileName() == null ? "totalcross-project" : plan.project().getFileName().toString();
    java.util.List<String> platforms = plan.deployArguments().stream().flatMap(item -> item.platforms().stream()).distinct().toList();
    java.util.List<String> deployArguments = plan.deployArguments().stream().flatMap(item -> item.arguments().stream()).toList();
    Map<Path, String> files = new LinkedHashMap<>();
    files.put(Path.of("settings.gradle"), "rootProject.name = '" + quote(projectName) + "'\n");
    files.put(Path.of("build.gradle"), "plugins {\n    id 'java'\n    id 'com.totalcross.application' version '" + quote(pluginVersion) + "'\n}\n\n"
        + "repositories {\n    maven { url = uri('https://maven.totalcross.com/artifactory/repo1') }\n    mavenCentral()\n}\n\n"
        + "java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }\n"
        + "tasks.withType(JavaCompile).configureEach { options.release = " + target + " }\n\n"
        + "dependencies { implementation 'com.totalcross:totalcross-sdk:" + quote(sdk) + "' }\n\n"
        + "totalcross {\n    applicationName = '" + quote(mainClass) + "'\n    sdkVersion = '" + quote(sdk) + "'"
        + (platforms.isEmpty() ? "" : "\n    platforms = " + strings(platforms))
        + (deployArguments.isEmpty() ? "" : "\n    deployArguments = " + strings(deployArguments)) + "\n}\n");
    return Map.copyOf(files);
  }

  private static String quote(String value) { return value.replace("\\", "\\\\").replace("'", "\\'"); }
  private static String strings(java.util.List<String> values) { return values.stream().map(value -> "'" + quote(value) + "'").collect(java.util.stream.Collectors.joining(", ", "[", "]")); }
}
