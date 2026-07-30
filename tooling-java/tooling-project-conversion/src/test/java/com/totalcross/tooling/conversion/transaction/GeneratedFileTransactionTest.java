/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.transaction;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeneratedFileTransactionTest {
  @TempDir Path project;

  @Test void creates_and_rolls_back_only_files_owned_by_the_transaction() throws Exception {
    var transaction = new GeneratedFileTransaction();
    var created = transaction.apply(project, Map.of(Path.of("settings.gradle"), "rootProject.name = 'demo'\n"));
    assertTrue(Files.isRegularFile(project.resolve("settings.gradle")));
    transaction.rollback(created);
    assertFalse(Files.exists(project.resolve("settings.gradle")));
  }

  @Test void rejects_an_existing_file_without_overwriting_it() throws Exception {
    Path build = project.resolve("build.gradle");
    Files.writeString(build, "unrelated\n");
    assertThrows(java.io.IOException.class, () -> new GeneratedFileTransaction().apply(project, Map.of(Path.of("build.gradle"), "generated\n")));
    assertEquals("unrelated\n", Files.readString(build));
  }
}
