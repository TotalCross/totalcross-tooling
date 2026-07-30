/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.inference;

import static org.junit.jupiter.api.Assertions.*;
import com.totalcross.tooling.conversion.plan.ConversionPlanCodec;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectConversionAnalyzerTest {
  @TempDir Path directory;

  @Test void produces_a_versioned_dry_run_plan_without_mutating_the_project() throws Exception {
    Path source = directory.resolve("app/App.java");
    Files.createDirectories(source.getParent());
    Files.writeString(source, "package demo; public class App extends totalcross.ui.MainWindow {}");
    Files.writeString(directory.resolve("legacy.sh"), "java totalcross.Launcher --release 7.6.0");
    var plan = new ProjectConversionAnalyzer().analyze(directory);
    assertEquals(1, plan.schemaVersion());
    assertEquals(1, plan.mainWindowCandidates().size());
    assertEquals("main-source", plan.moves().get(0).kind());
    assertFalse(Files.exists(directory.resolve("src/main/java/demo/App.java")));
    assertEquals("launcher", plan.scriptEvidence().get(0).kind());
    assertEquals("7.6.0", plan.sdkCandidates().get(0).version());
    assertTrue(new ConversionPlanCodec().toJson(plan).contains("scriptEvidence"));
  }
}
