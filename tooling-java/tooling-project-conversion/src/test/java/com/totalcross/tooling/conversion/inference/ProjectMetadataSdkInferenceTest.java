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

class ProjectMetadataSdkInferenceTest {
  @TempDir Path project;

  @Test void reads_literal_sdk_version_from_totalcross_metadata() throws Exception {
    Path metadata = project.resolve(".totalcross/project.json");
    Files.createDirectories(metadata.getParent());
    Files.writeString(metadata, "{\n  \"sdkVersion\": \"7.6.0\"\n}\n");
    var candidate = new ProjectMetadataSdkInference().infer(project).get(0);
    assertEquals("7.6.0", candidate.version());
    assertEquals(2, candidate.line());
    assertEquals("project-metadata", candidate.evidenceKind());
  }
}
