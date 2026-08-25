// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.totalcross.tooling.build.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ToolingCliTest {
  @Test
  void buildsTheWorkerJavaCommandFromTheSelectedHome() {
    String executable = ToolingCli.javaExecutable(Path.of("/tmp/selected-jdk"));
    String name = System.getProperty("os.name", "").toLowerCase().startsWith("windows") ? "java.exe" : "java";
    assertEquals(Path.of("/tmp/selected-jdk", "bin", name).toString(), executable);
  }

  @Test
  void makesOnlyPreviewWorkersHeadless() throws Exception {
    Path selectedJdk = Path.of("/tmp/selected-jdk");
    List<String> preview = ToolingCli.workerCommand(selectedJdk, true);
    List<String> run = ToolingCli.workerCommand(selectedJdk, false);

    assertTrue(preview.contains("-Djava.awt.headless=true"));
    assertFalse(run.contains("-Djava.awt.headless=true"));
    assertEquals(com.totalcross.tooling.worker.PreviewWorkerMain.class.getName(), preview.get(preview.size() - 1));
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
  void runtimeClasspathIncludesEachPublishedToolingModuleOnce() throws Exception {
    String[] entries = ToolingCli.runtimeClasspath().split(java.util.regex.Pattern.quote(java.io.File.pathSeparator));
    Set<String> locations = new HashSet<>(Arrays.asList(entries));
    assertEquals(entries.length, locations.size());
    assertTrue(locations.contains(codeSource(ToolingCli.class)));
    assertTrue(locations.contains(codeSource(ProjectModelCodec.class)));
    assertTrue(locations.contains(codeSource(com.totalcross.tooling.conversion.inference.ProjectConversionAnalyzer.class)));
    assertTrue(locations.contains(codeSource(com.totalcross.tooling.host.PreviewHost.class)));
    assertTrue(locations.contains(codeSource(com.totalcross.tooling.worker.PreviewWorkerMain.class)));
    assertTrue(locations.contains(codeSource(com.totalcross.tooling.protocol.ProtocolCodec.class)));
  }

  @Test
  void forkedCliStartsWithOnlyTheReturnedRuntimeClasspath() throws Exception {
    Process process = new ProcessBuilder(javaExecutable(), "-cp", ToolingCli.runtimeClasspath(),
        ToolingCli.class.getName(), "--help").redirectErrorStream(true).start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    assertEquals(0, process.waitFor());
    assertTrue(output.contains("usage: totalcross-tooling"), output);
  }

  @Test
  void forkedPreviewReadsAProjectModelBeforeStartingTheWorker() throws Exception {
    Path project = Files.createTempDirectory("totalcross-cli-model-");
    try {
      Path classes = project.resolve("build/classes/java/main");
      Files.createDirectories(classes);
      Path modelFile = project.resolve("build/totalcross/project-model.json");
      Files.createDirectories(modelFile.getParent());
      ProjectModel model = new ProjectModel(BuildTool.GRADLE, project, List.of(), List.of(), List.of(),
          new ClassOutput(classes), new ClassOutput(project.resolve("build/classes/java/test")),
          new DependencyClasspath(List.of()), "example.MainWindow", "7.3.0",
          "com.totalcross:totalcross-sdk:7.3.0", "catalog:java-17",
          new JavaCompatibilityPolicy(17, 17, 17), new RetrolambdaPlan(false, "modern"), List.of(), List.of(), List.of(),
          new PreviewSessionDescriptor(1, BuildTool.GRADLE, project, modelFile, "example.MainWindow", "7.3.0"));
      Files.writeString(modelFile, new ProjectModelCodec().toJson(model));

      Process process = new ProcessBuilder(javaExecutable(), "-cp", ToolingCli.runtimeClasspath(),
          ToolingCli.class.getName(), "preview", "--model", modelFile.toString(), "--project", project.toString(),
          "--classpath", classes.toString(), "--jdk-path", System.getProperty("java.home"), "--once")
          .redirectErrorStream(true).start();
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      assertTrue(process.waitFor() != 0, output);
      assertFalse(output.contains("NoClassDefFoundError"), output);
      assertFalse(output.contains("ProjectModelCodec"), output);
    } finally {
      try (var files = Files.walk(project)) { files.sorted(java.util.Comparator.reverseOrder()).forEach(path -> path.toFile().delete()); }
    }
  }

  @Test
  void analyzes_a_non_gradle_project_through_the_shared_conversion_module() throws Exception {
    Path project = Files.createTempDirectory("totalcross-convert-cli-");
    try {
      Path source = project.resolve("App.java");
      Files.writeString(source, "public class App extends totalcross.ui.MainWindow {}");
      Files.writeString(project.resolve("legacy.sh"), "java totalcross.Launcher 7.6.0\n");
      assertEquals(1, ConvertProjectCommand.analyze(project).mainWindowCandidates().size());
      assertEquals(0, Files.list(project).filter(path -> path.getFileName().toString().equals("src")).count());
    } finally {
      try (var files = Files.walk(project)) { files.sorted(java.util.Comparator.reverseOrder()).forEach(path -> path.toFile().delete()); }
    }
  }

  @Test
  void applies_only_a_saved_plan_that_still_matches_the_project() throws Exception {
    Path project = Files.createTempDirectory("totalcross-convert-apply-");
    try {
      Path source = project.resolve("App.java");
      Files.writeString(source, "public class App extends totalcross.ui.MainWindow {}");
      Files.writeString(project.resolve("legacy.sh"), "java totalcross.Launcher 7.6.0\n");
      Path plan = Files.createTempFile("totalcross-convert-apply-plan-", ".json");
      Files.writeString(plan, new com.totalcross.tooling.conversion.plan.ConversionPlanCodec().toJson(ConvertProjectCommand.analyze(project)));
      var result = ConvertProjectCommand.apply(plan);
      assertEquals(1, result.movedFiles());
      assertEquals(false, Files.exists(source));
      assertEquals(true, Files.exists(project.resolve("build.gradle")));
      assertEquals(true, Files.isExecutable(project.resolve("gradlew")));
      assertEquals(1, new com.totalcross.tooling.conversion.transaction.ProjectConversionTransaction().rollback(result.journal()));
      assertEquals(true, Files.exists(source));
      assertEquals(false, Files.exists(project.resolve("build.gradle")));
      assertEquals(false, Files.exists(project.resolve("gradlew")));
      Files.deleteIfExists(plan);
    } finally {
      try (var files = Files.walk(project)) { files.sorted(java.util.Comparator.reverseOrder()).forEach(path -> path.toFile().delete()); }
    }
  }

  @Test
  void bootstraps_a_non_gradle_project_through_the_public_cli_commands() throws Exception {
    Path project = Files.createTempDirectory("totalcross-cli-bootstrap-");
    Path plan = Files.createTempFile("totalcross-cli-bootstrap-plan-", ".json");
    try {
      Path source = project.resolve("App.java");
      Files.writeString(source, "public class App extends totalcross.ui.MainWindow {}");
      Files.writeString(project.resolve("legacy.sh"), "java totalcross.Launcher 7.6.0\n");
      String analysis = capture(() -> ToolingCli.main(new String[] {"convert-project", "analyze", "--project", project.toString(), "--plan", plan.toString()}));
      assertTrue(analysis.contains("\"event\":\"conversion-plan\""));
      String applied = capture(() -> ToolingCli.main(new String[] {"convert-project", "apply", "--plan", plan.toString()}));
      String journal = applied.replaceFirst(".*\\\"journal\\\":\\\"([^\\\"]+)\\\".*", "$1").trim();
      assertTrue(applied.contains("\"event\":\"conversion-applied\""));
      assertFalse(Files.exists(source));
      assertTrue(Files.isRegularFile(project.resolve("build.gradle")));
      String rollback = capture(() -> ToolingCli.main(new String[] {"convert-project", "rollback", "--journal", journal}));
      assertTrue(rollback.contains("\"event\":\"conversion-rolled-back\""));
      assertTrue(Files.isRegularFile(source));
      assertFalse(Files.exists(project.resolve("build.gradle")));
    } finally {
      Files.deleteIfExists(plan);
      try (var files = Files.walk(project)) { files.sorted(java.util.Comparator.reverseOrder()).forEach(path -> path.toFile().delete()); }
    }
  }

  private static String capture(ThrowingRunnable command) throws Exception {
    PrintStream previous = System.out;
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try { System.setOut(new PrintStream(output)); command.run(); return output.toString(); }
    finally { System.setOut(previous); }
  }

  private static String javaExecutable() {
    String name = System.getProperty("os.name", "").toLowerCase().startsWith("win") ? "java.exe" : "java";
    return Path.of(System.getProperty("java.home"), "bin", name).toString();
  }

  private static String codeSource(Class<?> type) throws Exception {
    return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI())
        .toAbsolutePath().normalize().toString();
  }

  @FunctionalInterface private interface ThrowingRunnable { void run() throws Exception; }
}
