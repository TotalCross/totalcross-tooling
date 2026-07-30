/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.script;

import java.nio.file.Path;
import java.util.List;

/** Reviewed command arguments with typed platform values separated from ordered passthrough options. */
public record LegacyArgumentMapping(Path script, int line, List<String> platforms, List<String> arguments) {
  public LegacyArgumentMapping { script = script.normalize(); platforms = List.copyOf(platforms); arguments = List.copyOf(arguments); }
}
