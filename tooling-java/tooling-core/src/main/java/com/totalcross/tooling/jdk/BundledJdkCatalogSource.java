// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import java.io.IOException;
import java.io.InputStream;

/** The reviewed catalog bundled in the tooling artifact. */
public final class BundledJdkCatalogSource implements JdkCatalogSource {
  public static final String RESOURCE = "/com/totalcross/tooling/jdk/jdk-catalog-v1.properties";

  @Override public String description() { return "bundled " + RESOURCE; }

  @Override public InputStream open() throws IOException {
    InputStream input = BundledJdkCatalogSource.class.getResourceAsStream(RESOURCE);
    if (input == null) throw new IOException("Missing bundled JDK catalog: " + RESOURCE);
    return input;
  }
}
