// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.build;

import java.nio.file.Path;

public record SourceRoot(Path path) {
  public SourceRoot { path = path.toAbsolutePath().normalize(); }
}
