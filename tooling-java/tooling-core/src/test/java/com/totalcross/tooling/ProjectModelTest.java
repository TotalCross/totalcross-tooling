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
  void normalizesRootsAndSerializesSessionSemantics() {
    Path project = Path.of(".");
    Path descriptor = project.resolve("build/session.json");
    PreviewSessionDescriptor session = new PreviewSessionDescriptor(1, BuildTool.GRADLE, project, descriptor, "App", "7.3");
    ProjectModel model = new ProjectModel(BuildTool.GRADLE, project, List.of(new SourceRoot(project.resolve("src"))),
        List.of(new ResourceRoot(project.resolve("resources"))), new ClassOutput(project.resolve("classes")),
        new DependencyClasspath(List.of()), new JavaCompatibilityPolicy(17, 17, 8), new RetrolambdaPlan(false, "modern"), session);

    assertTrue(model.project().isAbsolute());
    assertTrue(session.toJson().contains("\"buildTool\":\"GRADLE\""));
    assertTrue(session.toJson().contains("\"mainClass\":\"App\""));
  }
}
