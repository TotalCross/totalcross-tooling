/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Separates deploy platform switches from ordered non-secret legacy arguments. */
public final class LegacyArgumentInference {
  private static final Set<String> PLATFORMS = Set.of("-win32", "-linux", "-macos", "-android", "-ios", "-linux_arm");
  private static final Set<String> SECRET_OPTIONS = Set.of("/r", "-r", "/m", "-m");

  public List<LegacyArgumentMapping> infer(List<LegacyScriptEvidence> evidence, String kind) {
    return evidence.stream().filter(item -> kind.equals(item.kind())).map(this::map).toList();
  }

  private LegacyArgumentMapping map(LegacyScriptEvidence evidence) {
    List<String> platforms = new ArrayList<>(), arguments = new ArrayList<>();
    int command = evidence.arguments().indexOf("launcher".equals(evidence.kind()) ? "totalcross.Launcher" : "tc.Deploy");
    List<String> tail = command < 0 ? List.of() : evidence.arguments().subList(command + 1, evidence.arguments().size());
    for (int index = 0; index < tail.size(); index++) {
      String value = tail.get(index);
      if (SECRET_OPTIONS.contains(value.toLowerCase(java.util.Locale.ROOT))) { index++; continue; }
      if (PLATFORMS.contains(value.toLowerCase(java.util.Locale.ROOT))) platforms.add(value);
      else if (!"<redacted>".equals(value)) arguments.add(value);
    }
    return new LegacyArgumentMapping(evidence.script(), evidence.line(), platforms, arguments);
  }
}
