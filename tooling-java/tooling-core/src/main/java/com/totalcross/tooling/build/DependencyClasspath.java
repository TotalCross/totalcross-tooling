// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.build;

import java.nio.file.Path;
import java.util.List;

public record DependencyClasspath(List<Path> entries) {
  public DependencyClasspath { entries = entries.stream().map(p -> p.toAbsolutePath().normalize()).toList(); }
}
