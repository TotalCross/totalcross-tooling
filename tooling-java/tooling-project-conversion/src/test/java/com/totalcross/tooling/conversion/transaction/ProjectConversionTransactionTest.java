/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.transaction;

import static org.junit.jupiter.api.Assertions.*;
import com.totalcross.tooling.conversion.inference.ProjectConversionAnalyzer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectConversionTransactionTest {
  @TempDir Path directory;

  @Test void moves_files_only_after_fingerprint_check_and_rolls_them_back() throws Exception {
    Path source = directory.resolve("legacy/App.java");
    Files.createDirectories(source.getParent());
    Files.writeString(source, "package app; public class App extends totalcross.ui.MainWindow {}");
    var plan = new ProjectConversionAnalyzer().analyze(directory);
    var transaction = new ProjectConversionTransaction();
    var result = transaction.apply(plan);
    Path destination = directory.resolve("src/main/java/app/App.java");
    assertFalse(Files.exists(source));
    assertTrue(Files.exists(destination));
    transaction.rollback(result, directory);
    assertTrue(Files.exists(source));
    assertFalse(Files.exists(destination));
  }

  @Test void rejects_analyzed_plans_after_the_project_changes() throws Exception {
    Path source = directory.resolve("App.java");
    Files.writeString(source, "public class App extends totalcross.ui.MainWindow {}");
    var plan = new ProjectConversionAnalyzer().analyze(directory);
    Files.writeString(directory.resolve("later.txt"), "changed");
    assertThrows(java.io.IOException.class, () -> new ProjectConversionTransaction().apply(plan));
  }
}
