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

class ExistingSdkVersionInferenceTest {
  @TempDir Path project;

  @Test void finds_literal_gradle_and_maven_sdk_coordinates_with_provenance() throws Exception {
    Files.writeString(project.resolve("build.gradle"), "implementation 'com.totalcross:totalcross-sdk:7.6.0'");
    var gradle = new ExistingSdkVersionInference().infer(project);
    assertEquals("7.6.0", gradle.get(0).version());
    assertEquals("existing-gradle", gradle.get(0).evidenceKind());
    Files.delete(project.resolve("build.gradle"));
    Files.writeString(project.resolve("pom.xml"), "<dependency><groupId>com.totalcross</groupId><artifactId>totalcross-sdk</artifactId><version>7.3.0</version></dependency>");
    var maven = new ExistingSdkVersionInference().infer(project);
    assertEquals("7.3.0", maven.get(0).version());
    assertEquals("existing-maven", maven.get(0).evidenceKind());
  }
}
