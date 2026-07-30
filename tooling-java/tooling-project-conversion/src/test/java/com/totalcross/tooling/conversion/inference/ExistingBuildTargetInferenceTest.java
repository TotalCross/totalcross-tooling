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

class ExistingBuildTargetInferenceTest {
  @TempDir Path project;

  @Test void reads_literal_gradle_and_maven_java_targets() throws Exception {
    Files.writeString(project.resolve("build.gradle"), "java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }");
    assertEquals(17, new ExistingBuildTargetInference().infer(project).getAsInt());
    Files.delete(project.resolve("build.gradle"));
    Files.writeString(project.resolve("pom.xml"), "<maven.compiler.source>1.8</maven.compiler.source><maven.compiler.target>8</maven.compiler.target>");
    assertEquals(8, new ExistingBuildTargetInference().infer(project).getAsInt());
  }
}
