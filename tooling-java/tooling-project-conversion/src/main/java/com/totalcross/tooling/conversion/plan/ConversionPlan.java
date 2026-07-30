/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.plan;

import java.nio.file.Path;
import java.util.List;

/** Immutable dry-run result; an apply operation must verify its fingerprint before mutation. */
public record ConversionPlan(int schemaVersion, Path project, String inventoryFingerprint,
    List<Move> moves, List<MainWindowCandidate> mainWindowCandidates, List<String> warnings) {
  public static final int SCHEMA_VERSION = 1;

  public ConversionPlan {
    if (schemaVersion != SCHEMA_VERSION) throw new IllegalArgumentException("unsupported conversion plan schema: " + schemaVersion);
    project = project.toAbsolutePath().normalize();
    moves = List.copyOf(moves);
    mainWindowCandidates = List.copyOf(mainWindowCandidates);
    warnings = List.copyOf(warnings);
  }

  public record Move(Path source, Path destination, String kind) {
    public Move { source = source.normalize(); destination = destination.normalize(); }
  }
  public record MainWindowCandidate(String className, Path source, String evidence) {
    public MainWindowCandidate { source = source.normalize(); }
  }
}
