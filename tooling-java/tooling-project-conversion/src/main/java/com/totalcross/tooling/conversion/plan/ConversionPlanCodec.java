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
        + ",\"mainWindowCandidates\":" + array(plan.mainWindowCandidates(), candidate -> "{\"className\":"
            + quote(candidate.className()) + ",\"source\":" + quote(candidate.source().toString()) + ",\"evidence\":"
            + quote(candidate.evidence()) + "}") + ",\"warnings\":" + array(plan.warnings(), this::quote) + "}";
  }

  private <T> String array(List<T> values, Function<T, String> encoder) {
    return "[" + values.stream().map(encoder).collect(java.util.stream.Collectors.joining(",")) + "]";
  }

  private String quote(String value) {
    String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    return "\"" + escaped + "\"";
  }
}
