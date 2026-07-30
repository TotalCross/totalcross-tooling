/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.transaction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Atomically creates reviewed generated files and removes only its own files during rollback. */
public final class GeneratedFileTransaction {
  public List<Path> apply(Path project, Map<Path, String> files) throws IOException {
    Path root = project.toAbsolutePath().normalize();
    List<Path> created = new ArrayList<>();
    try {
      for (var entry : files.entrySet()) {
        Path destination = within(root, entry.getKey());
        if (Files.exists(destination)) throw new IOException("generated-file collision: " + entry.getKey());
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), ".totalcross-conversion-", ".tmp");
        try {
          Files.writeString(temporary, entry.getValue());
          Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
          Files.move(temporary, destination);
        } finally { Files.deleteIfExists(temporary); }
        created.add(destination);
      }
      return List.copyOf(created);
    } catch (Exception failure) {
      rollback(created);
      throw failure instanceof IOException io ? io : new IOException("unable to generate conversion files", failure);
    }
  }

  public void rollback(List<Path> created) throws IOException {
    for (Path file : created.stream().sorted(java.util.Comparator.comparing(Path::getNameCount).reversed()).toList()) {
      Files.deleteIfExists(file);
    }
  }

  private static Path within(Path root, Path relative) throws IOException {
    Path destination = root.resolve(relative).normalize();
    if (!destination.startsWith(root)) throw new IOException("generated path escapes project: " + relative);
    return destination;
  }
}
