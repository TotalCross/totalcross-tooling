/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.plan;

import com.totalcross.tooling.conversion.script.LegacyScriptEvidence;
import com.totalcross.tooling.conversion.script.LegacyArgumentMapping;
import com.totalcross.tooling.conversion.inference.SdkVersionInference;
import java.nio.file.Path;
import java.util.List;

/** Immutable dry-run result; an apply operation must verify its fingerprint before mutation. */
public record ConversionPlan(int schemaVersion, Path project, String inventoryFingerprint,
    List<Move> moves, List<MainWindowCandidate> mainWindowCandidates, List<LegacyScriptEvidence> scriptEvidence,
    List<SdkVersionInference.Candidate> sdkCandidates, List<LegacyArgumentMapping> launcherArguments,
    List<LegacyArgumentMapping> deployArguments, List<String> warnings) {
  public static final int SCHEMA_VERSION = 1;

  public ConversionPlan {
    if (schemaVersion != SCHEMA_VERSION) throw new IllegalArgumentException("unsupported conversion plan schema: " + schemaVersion);
    project = project.toAbsolutePath().normalize();
    moves = List.copyOf(moves);
    mainWindowCandidates = List.copyOf(mainWindowCandidates);
    scriptEvidence = List.copyOf(scriptEvidence);
    sdkCandidates = List.copyOf(sdkCandidates);
    launcherArguments = List.copyOf(launcherArguments);
    deployArguments = List.copyOf(deployArguments);
    warnings = List.copyOf(warnings);
  }

  public record Move(Path source, Path destination, String kind) {
    public Move { source = source.normalize(); destination = destination.normalize(); }
  }
  public record MainWindowCandidate(String className, Path source, String evidence) {
    public MainWindowCandidate { source = source.normalize(); }
  }
}
