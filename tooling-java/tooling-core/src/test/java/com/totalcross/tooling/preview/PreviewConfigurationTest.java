// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PreviewConfigurationTest {
  @Test
  void readsCanonicalAndLegacyClasspathEntriesInStableDeduplicatedOrder() throws Exception {
    Path project = Files.createTempDirectory("preview-config-");
    try {
      Path file = project.resolve("totalcross-preview.json");
      Files.writeString(file, "{\"mainWindow\":\"demo.App\",\"launcherArgs\":[\"width\",\"360\"],"
          + "\"classpath\":[\"lib\",\"shared\"],\"classOutputPaths\":[\"shared\",\"build/classes\"]}");
      PreviewConfiguration configuration = PreviewConfiguration.read(file, project);
      assertEquals("demo.App", configuration.mainWindow());
      assertEquals(List.of("width", "360"), configuration.launcherArgs());
      assertEquals(List.of(project.resolve("lib"), project.resolve("shared"), project.resolve("build/classes")), configuration.classpath());
    } finally {
      try (var files = Files.walk(project)) { files.sorted(java.util.Comparator.reverseOrder()).forEach(path -> path.toFile().delete()); }
    }
  }
}
