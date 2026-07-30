/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.inference;

import static org.junit.jupiter.api.Assertions.*;
import com.totalcross.tooling.conversion.script.LegacyScriptEvidence;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class JavaTargetInferenceTest {
  @Test void prefers_literal_release_and_uses_the_sdk_ceiling_when_absent() {
    var javac = new LegacyScriptEvidence(Path.of("build.sh"), 1, "javac", List.of("javac", "--release", "11"), false);
    assertEquals(11, new JavaTargetInference().infer("7.6.0", List.of(javac)).target());
    assertEquals(8, new JavaTargetInference().infer("7.2.2", List.of()).target());
  }

  @Test void rejects_a_script_target_not_supported_by_the_sdk() {
    var javac = new LegacyScriptEvidence(Path.of("build.sh"), 1, "javac", List.of("javac", "--release", "17"), false);
    assertThrows(IllegalArgumentException.class, () -> new JavaTargetInference().infer("7.2.2", List.of(javac)));
  }
}
