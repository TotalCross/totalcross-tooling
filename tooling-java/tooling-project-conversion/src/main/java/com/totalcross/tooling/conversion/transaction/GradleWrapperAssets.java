/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.transaction;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Writes the versioned official Wrapper assets bundled in the conversion module. */
public final class GradleWrapperAssets {
  private static final List<String> ASSETS = List.of("gradlew", "gradlew.bat", "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties");

  public List<Path> write(Path project) throws IOException {
    Path root = project.toAbsolutePath().normalize();
    List<Path> created = new ArrayList<>();
    try {
      for (String asset : ASSETS) {
        Path destination = root.resolve(asset);
        if (Files.exists(destination)) throw new IOException("Gradle Wrapper collision: " + asset);
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), ".totalcross-wrapper-", ".tmp");
        try (InputStream source = required(asset)) {
          Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
          Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) { Files.move(temporary, destination); }
        finally { Files.deleteIfExists(temporary); }
        if (asset.equals("gradlew")) destination.toFile().setExecutable(true, false);
        created.add(destination);
      }
      return List.copyOf(created);
    } catch (Exception failure) {
      new GeneratedFileTransaction().rollback(created);
      throw failure instanceof IOException io ? io : new IOException("unable to write Gradle Wrapper", failure);
    }
  }

  private InputStream required(String asset) throws IOException {
    InputStream input = getClass().getResourceAsStream("/gradle-wrapper/" + asset);
    if (input == null) throw new IOException("bundled Gradle Wrapper asset is missing: " + asset);
    return input;
  }
}
