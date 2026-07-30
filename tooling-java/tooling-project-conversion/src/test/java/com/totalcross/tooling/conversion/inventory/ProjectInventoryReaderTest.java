/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.inventory;

import static org.junit.jupiter.api.Assertions.*;
import com.totalcross.tooling.conversion.java.JavaSourceClassifier;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectInventoryReaderTest {
  @TempDir Path directory;

  @Test void inventories_sources_without_build_output_and_classifies_java_layout() throws Exception {
    write("App.java", "package demo; public class App extends totalcross.ui.MainWindow {}");
    write("test/AppTest.java", "package demo; import org.junit.Test; class AppTest {}");
    write("assets/logo.png", "bytes");
    write("build/ignored.java", "class Ignored {}");
    ProjectInventory inventory = new ProjectInventoryReader().read(directory);
    assertEquals(3, inventory.entries().size());
    assertFalse(inventory.fingerprint().isBlank());
    JavaSourceClassifier.Classification classification = new JavaSourceClassifier().classify(inventory);
    assertEquals(Path.of("src/main/java/demo/App.java"), classification.mainSources().get(0).destination());
    assertEquals(Path.of("src/test/java/demo/AppTest.java"), classification.testSources().get(0).destination());
    assertEquals("demo.App", classification.mainWindowCandidates().get(0).className());
    assertEquals(1, classification.resources().size());
  }

  private void write(String relative, String contents) throws Exception {
    Path file = directory.resolve(relative);
    Files.createDirectories(file.getParent() == null ? directory : file.getParent());
    Files.writeString(file, contents);
  }
}
