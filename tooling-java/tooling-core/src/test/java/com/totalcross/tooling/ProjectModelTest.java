// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling;

import static org.junit.jupiter.api.Assertions.*;
import com.totalcross.tooling.build.*;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectModelTest {
  @Test
  void normalizesRootsAndRoundTripsTheCompleteModel() {
    Path project = Path.of(".");
    Path descriptor = project.resolve("build/session.json");
    PreviewSessionDescriptor session = new PreviewSessionDescriptor(1, BuildTool.GRADLE, project, descriptor, "App", "7.3");
    ProjectModel model = new ProjectModel(BuildTool.GRADLE, project, List.of(new SourceRoot(project.resolve("src"))),
        List.of(new SourceRoot(project.resolve("test"))), List.of(new ResourceRoot(project.resolve("resources"))),
        new ClassOutput(project.resolve("classes")), new ClassOutput(project.resolve("test-classes")), new DependencyClasspath(List.of()),
        "sample.App", "7.3", "com.totalcross:totalcross-sdk:7.3", "catalog:temurin-17", new JavaCompatibilityPolicy(17, 17, 8),
        new RetrolambdaPlan(false, "modern"), List.of("/m"), List.of("/n", "App"), List.of("linux"), session);
    String json = new ProjectModelCodec().toJson(model);
    ProjectModel decoded = new ProjectModelCodec().fromJson(json);

    assertTrue(model.project().isAbsolute());
    assertEquals(model, decoded);
    assertTrue(json.contains("\"schemaVersion\":1"));
    assertTrue(json.contains("\"mainClass\":\"sample.App\""));
  }

  @Test void rejectsAnUnknownSchema() { assertThrows(IllegalArgumentException.class, () -> new ProjectModelCodec().fromJson("{\"schemaVersion\":2}")); }
}
