/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.script;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class LegacyScriptAnalyzerTest {
  @Test void recognizes_literal_commands_expands_same_file_variables_and_redacts_secrets() {
    var evidence = new LegacyScriptAnalyzer().analyzeFile(Path.of("build.cmd"), List.of(
        "set SDK=7.6.0", "javac --release 17 App.java", "java totalcross.Launcher /sdk/$SDK",
        "java tc.Deploy -activationKey=private app.jar", "java $(unsafe) totalcross.Launcher"));
    assertEquals(List.of("javac", "launcher", "deploy"), evidence.stream().map(LegacyScriptEvidence::kind).toList());
    assertTrue(evidence.get(1).arguments().contains("/sdk/7.6.0"));
    assertTrue(evidence.get(2).secretPresent());
    assertFalse(String.join(" ", evidence.get(2).arguments()).contains("private"));
  }

  @Test void separates_deploy_platforms_and_omits_short_secret_options() {
    var evidence = new LegacyScriptAnalyzer().analyzeFile(Path.of("build.sh"), List.of(
        "java tc.Deploy App -android -linux /r private /q"));
    assertTrue(evidence.get(0).secretPresent());
    var mapping = new LegacyArgumentInference().infer(evidence, "deploy").get(0);
    assertEquals(List.of("-android", "-linux"), mapping.platforms());
    assertEquals(List.of("/q"), mapping.arguments());
  }
}
