/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.plan;

import java.util.List;
import java.util.function.Function;

/** Small dependency-free JSON writer for the public conversion-plan schema. */
public final class ConversionPlanCodec {
  public String toJson(ConversionPlan plan) {
    return "{\"schemaVersion\":" + plan.schemaVersion() + ",\"project\":" + quote(plan.project().toString())
        + ",\"inventoryFingerprint\":" + quote(plan.inventoryFingerprint()) + ",\"moves\":"
        + array(plan.moves(), move -> "{\"source\":" + quote(move.source().toString()) + ",\"destination\":"
            + quote(move.destination().toString()) + ",\"kind\":" + quote(move.kind()) + "}")
        + ",\"generatedFiles\":" + array(plan.generatedFiles(), file -> "{\"destination\":" + quote(file.destination().toString())
            + ",\"kind\":" + quote(file.kind()) + "}")
        + ",\"mainWindowCandidates\":" + array(plan.mainWindowCandidates(), candidate -> "{\"className\":"
            + quote(candidate.className()) + ",\"source\":" + quote(candidate.source().toString()) + ",\"evidence\":"
            + quote(candidate.evidence()) + "}") + ",\"scriptEvidence\":"
        + array(plan.scriptEvidence(), evidence -> "{\"script\":" + quote(evidence.script().toString()) + ",\"line\":"
            + evidence.line() + ",\"kind\":" + quote(evidence.kind()) + ",\"arguments\":"
            + array(evidence.arguments(), this::quote) + ",\"secretPresent\":" + evidence.secretPresent() + "}")
        + ",\"sdkCandidates\":" + array(plan.sdkCandidates(), candidate -> "{\"version\":" + quote(candidate.version())
            + ",\"script\":" + quote(candidate.script().toString()) + ",\"line\":" + candidate.line()
            + ",\"evidenceKind\":" + quote(candidate.evidenceKind()) + "}")
        + ",\"launcherArguments\":" + mappings(plan.launcherArguments())
        + ",\"deployArguments\":" + mappings(plan.deployArguments())
        + ",\"warnings\":" + array(plan.warnings(), this::quote) + "}";
  }

  private String mappings(List<com.totalcross.tooling.conversion.script.LegacyArgumentMapping> values) {
    return array(values, value -> "{\"script\":" + quote(value.script().toString()) + ",\"line\":" + value.line()
        + ",\"platforms\":" + array(value.platforms(), this::quote) + ",\"arguments\":" + array(value.arguments(), this::quote) + "}");
  }

  private <T> String array(List<T> values, Function<T, String> encoder) {
    return "[" + values.stream().map(encoder).collect(java.util.stream.Collectors.joining(",")) + "]";
  }

  private String quote(String value) {
    String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    return "\"" + escaped + "\"";
  }
}
