/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.inference;

import static org.junit.jupiter.api.Assertions.*;
import com.totalcross.tooling.conversion.script.LegacyScriptEvidence;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JavaTargetInferenceTest {
  @TempDir Path project;

  @Test void prefers_literal_release_and_uses_the_sdk_ceiling_when_absent() {
    var javac = new LegacyScriptEvidence(Path.of("build.sh"), 1, "javac", List.of("javac", "--release", "11"), false);
    assertEquals(11, new JavaTargetInference().infer("7.6.0", List.of(javac)).target());
    assertEquals(8, new JavaTargetInference().infer("7.2.2", List.of()).target());
  }

  @Test void rejects_a_script_target_not_supported_by_the_sdk() {
    var javac = new LegacyScriptEvidence(Path.of("build.sh"), 1, "javac", List.of("javac", "--release", "17"), false);
    assertThrows(IllegalArgumentException.class, () -> new JavaTargetInference().infer("7.2.2", List.of(javac)));
  }

  @Test void accepts_consistent_source_and_target_and_rejects_conflicting_compilers() {
    var legacy = new LegacyScriptEvidence(Path.of("build.bat"), 1, "javac", List.of("javac", "-source", "1.8", "-target", "8"), false);
    assertEquals(8, new JavaTargetInference().infer("7.2.2", List.of(legacy)).target());
    var release = new LegacyScriptEvidence(Path.of("build.sh"), 2, "javac", List.of("javac", "--release", "17"), false);
    assertThrows(IllegalArgumentException.class, () -> new JavaTargetInference().infer("7.6.0", List.of(legacy, release)));
  }

  @Test void uses_compiled_output_only_when_compiler_flags_are_absent() throws Exception {
    Path output = project.resolve("bin/App.class");
    Files.createDirectories(output.getParent());
    Files.write(output, new byte[] {(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe, 0, 0, 0, 55});
    var explicit = new LegacyScriptEvidence(Path.of("build.sh"), 1, "javac", List.of("javac", "--release", "8"), false);
    assertEquals(11, new JavaTargetInference().infer("7.6.0", List.of(), project).target());
    assertEquals(8, new JavaTargetInference().infer("7.6.0", List.of(explicit), project).target());
  }
}
