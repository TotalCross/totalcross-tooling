/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.inference;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClassfileTargetInferenceTest {
  @TempDir Path project;

  @Test void selects_the_highest_target_from_common_compiled_output_roots() throws Exception {
    write("bin/Old.class", 52);
    write("target/classes/New.class", 61);
    assertEquals(17, new ClassfileTargetInference().infer(project).getAsInt());
  }

  private void write(String relative, int major) throws Exception {
    Path file = project.resolve(relative);
    Files.createDirectories(file.getParent());
    Files.write(file, new byte[] {(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe, 0, 0, (byte) (major >> 8), (byte) major});
  }
}
