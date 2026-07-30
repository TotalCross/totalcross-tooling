/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.inference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.totalcross.tooling.conversion.script.LegacyScriptEvidence;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class SdkVersionInferenceTest {
  @Test void exposes_versions_as_evidence_without_selecting_an_ambiguous_one() {
    var launcher = new LegacyScriptEvidence(Path.of("legacy.sh"), 3, "launcher", List.of("java", "totalcross.Launcher", "7.6.0"), false);
    var deploy = new LegacyScriptEvidence(Path.of("legacy.sh"), 4, "deploy", List.of("java", "tc.Deploy", "7.5.1"), false);
    assertEquals(List.of("7.6.0", "7.5.1"), new SdkVersionInference().infer(List.of(launcher, deploy)).stream()
        .map(SdkVersionInference.Candidate::version).toList());
  }
}
