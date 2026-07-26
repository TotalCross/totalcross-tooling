// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.build;

import java.nio.file.Path;

public record ResourceRoot(Path path) {
  public ResourceRoot { path = path.toAbsolutePath().normalize(); }
}
