/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.transaction;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GradleWrapperAssetsTest {
  @TempDir Path project;

  @Test void writes_complete_official_wrapper_assets_without_overwriting_files() throws Exception {
    var files = new GradleWrapperAssets().write(project);
    assertEquals(4, files.size());
    assertTrue(Files.isExecutable(project.resolve("gradlew")));
    assertTrue(Files.size(project.resolve("gradle/wrapper/gradle-wrapper.jar")) > 0);
    assertThrows(java.io.IOException.class, () -> new GradleWrapperAssets().write(project));
  }
}
