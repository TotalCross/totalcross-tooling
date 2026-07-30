/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.inference;

import static org.junit.jupiter.api.Assertions.*;
import com.totalcross.tooling.conversion.plan.ConversionPlanCodec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectConversionAnalyzerTest {
  @TempDir Path directory;

  @Test void produces_a_versioned_dry_run_plan_without_mutating_the_project() throws Exception {
    Path source = directory.resolve("app/App.java");
    Files.createDirectories(source.getParent());
    Files.writeString(source, "package demo; public class App extends totalcross.ui.MainWindow {}");
    Files.writeString(directory.resolve("legacy.sh"), "java totalcross.Launcher --release 7.6.0\njava tc.Deploy App -android /q");
    var plan = new ProjectConversionAnalyzer().analyze(directory);
    assertEquals(1, plan.schemaVersion());
    assertEquals(1, plan.mainWindowCandidates().size());
    assertEquals("main-source", plan.moves().get(0).kind());
    assertFalse(Files.exists(directory.resolve("src/main/java/demo/App.java")));
    assertEquals("launcher", plan.scriptEvidence().get(0).kind());
    assertEquals("7.6.0", plan.sdkCandidates().get(0).version());
    assertEquals(List.of("-android"), plan.deployArguments().get(0).platforms());
    assertEquals(List.of("/q"), plan.deployArguments().get(0).arguments());
    assertTrue(new ConversionPlanCodec().toJson(plan).contains("scriptEvidence"));
    assertTrue(new ConversionPlanCodec().toJson(plan).contains("deployArguments"));
  }

  @Test void prefers_an_existing_sdk_coordinate_to_legacy_script_evidence() throws Exception {
    Files.writeString(directory.resolve("App.java"), "public class App extends totalcross.ui.MainWindow {}");
    Files.writeString(directory.resolve("build.gradle"), "implementation 'com.totalcross:totalcross-sdk:7.6.0'");
    Files.writeString(directory.resolve("legacy.sh"), "java totalcross.Launcher 7.3.0");
    var plan = new ProjectConversionAnalyzer().analyze(directory);
    assertEquals("7.6.0", plan.sdkCandidates().get(0).version());
    assertEquals("existing-gradle", plan.sdkCandidates().get(0).evidenceKind());
  }

  @Test void uses_project_metadata_only_after_script_sdk_evidence_is_absent() throws Exception {
    Files.writeString(directory.resolve("App.java"), "public class App extends totalcross.ui.MainWindow {}");
    Path metadata = directory.resolve(".totalcross/project.json");
    Files.createDirectories(metadata.getParent());
    Files.writeString(metadata, "{\"sdkVersion\":\"7.6.0\"}");
    assertEquals("7.6.0", new ProjectConversionAnalyzer().analyze(directory).sdkCandidates().get(0).version());
    Files.writeString(directory.resolve("legacy.sh"), "java totalcross.Launcher 7.3.0");
    assertEquals("7.3.0", new ProjectConversionAnalyzer().analyze(directory).sdkCandidates().get(0).version());
  }
}
