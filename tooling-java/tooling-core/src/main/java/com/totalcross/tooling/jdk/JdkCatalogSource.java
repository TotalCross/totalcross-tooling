// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import java.io.IOException;
import java.io.InputStream;

/** Opens catalog bytes without coupling parsing to one transport or classpath resource. */
public interface JdkCatalogSource {
  String description();
  InputStream open() throws IOException;
}
