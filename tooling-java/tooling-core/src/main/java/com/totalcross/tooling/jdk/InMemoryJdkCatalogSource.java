// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** A small immutable source for focused tests and catalog-maintenance validation. */
public record InMemoryJdkCatalogSource(String content) implements JdkCatalogSource {
  public InMemoryJdkCatalogSource {
    if (content == null) throw new IllegalArgumentException("catalog content is required");
  }
  @Override public String description() { return "in-memory JDK catalog"; }
  @Override public InputStream open() { return new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1)); }
}
