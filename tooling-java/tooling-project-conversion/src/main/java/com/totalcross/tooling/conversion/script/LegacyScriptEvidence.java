/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.script;

import java.nio.file.Path;
import java.util.List;

/** A redacted literal command found in a legacy script; it is never executed by conversion. */
public record LegacyScriptEvidence(Path script, int line, String kind, List<String> arguments, boolean secretPresent) {
  public LegacyScriptEvidence { script = script.normalize(); arguments = List.copyOf(arguments); }
}
