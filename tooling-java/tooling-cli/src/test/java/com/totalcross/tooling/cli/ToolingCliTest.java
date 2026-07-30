// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.totalcross.tooling.build.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToolingCliTest {
  @Test
  void buildsTheWorkerJavaCommandFromTheSelectedHome() {
    String executable = ToolingCli.javaExecutable(Path.of("/tmp/selected-jdk"));
    String name = System.getProperty("os.name", "").toLowerCase().startsWith("windows") ? "java.exe" : "java";
    assertEquals(Path.of("/tmp/selected-jdk", "bin", name).toString(), executable);
  }

  @Test
  void usesTheModelClasspathWithoutDirectoryDiscovery() {
    Path project = Path.of("/tmp/model-project");
    ProjectModel model = new ProjectModel(BuildTool.GRADLE, project, List.of(), List.of(),
        List.of(new ResourceRoot(project.resolve("resources"))), new ClassOutput(project.resolve("classes")),
        new ClassOutput(project.resolve("test-classes")), new DependencyClasspath(List.of(project.resolve("dependency.jar"))),
        "sample.MainWindow", "7.3.0", "com.totalcross:totalcross-sdk:7.3.0", "catalog:java-17",
        new JavaCompatibilityPolicy(17, 17, 17), new RetrolambdaPlan(false, "modern"), List.of(), List.of(), List.of(),
        new PreviewSessionDescriptor(1, BuildTool.GRADLE, project, project.resolve("preview-session.json"), "sample.MainWindow", "7.3.0"));

    assertEquals(String.join(java.io.File.pathSeparator, project.resolve("classes").toString(), project.resolve("resources").toString(),
        project.resolve("dependency.jar").toString()), ToolingCli.modelClasspath(model));
  }

  @Test
  void analyzes_a_non_gradle_project_through_the_shared_conversion_module() throws Exception {
    Path project = Files.createTempDirectory("totalcross-convert-cli-");
    try {
      Path source = project.resolve("App.java");
      Files.writeString(source, "public class App extends totalcross.ui.MainWindow {}");
      assertEquals(1, ConvertProjectCommand.analyze(project).mainWindowCandidates().size());
      assertEquals(0, Files.list(project).filter(path -> path.getFileName().toString().equals("src")).count());
    } finally {
      try (var files = Files.walk(project)) { files.sorted(java.util.Comparator.reverseOrder()).forEach(path -> path.toFile().delete()); }
    }
  }
}
