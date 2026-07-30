/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.inference;

import com.totalcross.tooling.conversion.inventory.ProjectInventory;
import com.totalcross.tooling.conversion.inventory.ProjectInventoryReader;
import com.totalcross.tooling.conversion.java.JavaSourceClassifier;
import com.totalcross.tooling.conversion.plan.ConversionPlan;
import com.totalcross.tooling.conversion.script.LegacyScriptAnalyzer;
import com.totalcross.tooling.conversion.script.LegacyArgumentInference;
import com.totalcross.tooling.conversion.script.LegacyScriptEvidence;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Coordinates read-only inventory and classification into an apply-safe conversion plan. */
public final class ProjectConversionAnalyzer {
  private final ProjectInventoryReader inventoryReader;
  private final JavaSourceClassifier classifier;
  private final LegacyScriptAnalyzer scriptAnalyzer;

  public ProjectConversionAnalyzer() { this(new ProjectInventoryReader(), new JavaSourceClassifier(), new LegacyScriptAnalyzer()); }
  ProjectConversionAnalyzer(ProjectInventoryReader inventoryReader, JavaSourceClassifier classifier, LegacyScriptAnalyzer scriptAnalyzer) {
    this.inventoryReader = inventoryReader;
    this.classifier = classifier;
    this.scriptAnalyzer = scriptAnalyzer;
  }

  public ConversionPlan analyze(Path project) throws IOException {
    ProjectInventory inventory = inventoryReader.read(project);
    JavaSourceClassifier.Classification layout = classifier.classify(inventory);
    List<ConversionPlan.Move> moves = new ArrayList<>();
    append(moves, layout.mainSources(), "main-source");
    append(moves, layout.testSources(), "test-source");
    append(moves, layout.resources(), "resource");
    List<ConversionPlan.MainWindowCandidate> candidates = layout.mainWindowCandidates().stream()
        .map(candidate -> new ConversionPlan.MainWindowCandidate(candidate.className(), candidate.source(), candidate.evidence())).toList();
    List<String> warnings = new ArrayList<>();
    layout.warnings().forEach(warning -> warnings.add(warning.source() + ": " + warning.evidence()));
    if (candidates.isEmpty()) warnings.add("No concrete public MainWindow candidate was detected; select one before apply.");
    if (candidates.size() > 1) warnings.add("Multiple MainWindow candidates were detected; select one before apply.");
    List<LegacyScriptEvidence> scriptEvidence = scriptAnalyzer.analyze(inventory.root());
    List<SdkVersionInference.Candidate> sdkCandidates = new SdkVersionInference().infer(scriptEvidence);
    if (scriptEvidence.stream().anyMatch(LegacyScriptEvidence::secretPresent)) {
      warnings.add("Legacy script secrets were redacted and must be configured locally after conversion.");
    }
    if (sdkCandidates.size() > 1) warnings.add("Multiple SDK versions were detected; select one before apply.");
    return new ConversionPlan(ConversionPlan.SCHEMA_VERSION, inventory.root(), inventory.fingerprint(), moves, candidates,
        scriptEvidence, sdkCandidates, new LegacyArgumentInference().infer(scriptEvidence, "launcher"),
        new LegacyArgumentInference().infer(scriptEvidence, "deploy"), warnings);
  }

  private static void append(List<ConversionPlan.Move> moves, List<JavaSourceClassifier.Source> sources, String kind) {
    sources.forEach(source -> moves.add(new ConversionPlan.Move(source.source(), source.destination(), kind)));
  }
}
