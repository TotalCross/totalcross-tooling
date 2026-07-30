/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.plan;

import static org.junit.jupiter.api.Assertions.*;
import com.totalcross.tooling.conversion.inference.ProjectConversionAnalyzer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GradleProjectRendererTest {
  @TempDir Path project;

  @Test void renders_a_conventional_project_from_unambiguous_evidence() throws Exception {
    Files.writeString(project.resolve("App.java"), "public class App extends totalcross.ui.MainWindow {}");
    Files.writeString(project.resolve("legacy.sh"), "javac --release 17 App.java\njava totalcross.Launcher 7.6.0\njava tc.Deploy App -android /q\n");
    var files = new GradleProjectRenderer().render(new ProjectConversionAnalyzer().analyze(project), "0.1.0");
    assertTrue(files.get(Path.of("settings.gradle")).contains("rootProject.name"));
    assertTrue(files.get(Path.of("build.gradle")).contains("totalcross-sdk:7.6.0"));
    assertTrue(files.get(Path.of("build.gradle")).contains("options.release = 17"));
    assertTrue(files.get(Path.of("build.gradle")).contains("platforms = ['-android']"));
    assertTrue(files.get(Path.of("build.gradle")).contains("deployArguments = ['/q']"));
  }

  @Test void resolves_the_catalog_sdk_only_when_the_plan_has_no_sdk_evidence() throws Exception {
    Files.writeString(project.resolve("App.java"), "public class App extends totalcross.ui.MainWindow {}");
    var files = new GradleProjectRenderer(() -> "7.6.0").render(new ProjectConversionAnalyzer().analyze(project), "0.1.0");
    assertTrue(files.get(Path.of("build.gradle")).contains("totalcross-sdk:7.6.0"));
  }
}
