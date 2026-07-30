/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.inference;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;

/** Reads class-file headers from common legacy output roots without loading project classes. */
public final class ClassfileTargetInference {
  private static final List<String> ROOTS = List.of("bin", "out", "target/classes", "build/classes/java/main");

  public OptionalInt infer(Path project) throws IOException {
    int highest = 0;
    Path root = project.toAbsolutePath().normalize();
    for (String relative : ROOTS) {
      Path output = root.resolve(relative);
      if (!Files.isDirectory(output)) continue;
      try (var paths = Files.walk(output)) {
        for (Path file : paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".class")).toList()) {
          highest = Math.max(highest, target(file));
        }
      }
    }
    return highest == 0 ? OptionalInt.empty() : OptionalInt.of(highest);
  }

  private static int target(Path file) throws IOException {
    try (InputStream input = Files.newInputStream(file)) {
      byte[] header = input.readNBytes(8);
      if (header.length != 8 || header[0] != (byte) 0xca || header[1] != (byte) 0xfe || header[2] != (byte) 0xba || header[3] != (byte) 0xbe) {
        throw new IOException("invalid class file: " + file);
      }
      return (((header[6] & 0xff) << 8) | (header[7] & 0xff)) - 44;
    }
  }
}
