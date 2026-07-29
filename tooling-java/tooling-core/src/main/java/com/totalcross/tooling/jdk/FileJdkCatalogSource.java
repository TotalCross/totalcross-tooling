// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** A local catalog file intended for reviewed tests or catalog-maintenance workflows. */
public record FileJdkCatalogSource(Path path) implements JdkCatalogSource {
  public FileJdkCatalogSource {
    if (path == null) throw new IllegalArgumentException("catalog path is required");
    path = path.toAbsolutePath().normalize();
  }
  @Override public String description() { return path.toString(); }
  @Override public InputStream open() throws IOException { return Files.newInputStream(path); }
}
