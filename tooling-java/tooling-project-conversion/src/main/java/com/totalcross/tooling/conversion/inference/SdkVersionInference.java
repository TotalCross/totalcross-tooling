/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.inference;

import com.totalcross.tooling.conversion.script.LegacyScriptEvidence;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts explicit semantic SDK versions from safe script evidence without guessing a release. */
public final class SdkVersionInference {
  private static final Pattern VERSION = Pattern.compile("(?<![\\d.])(\\d+\\.\\d+\\.\\d+)(?![\\d.])");

  public List<Candidate> infer(List<LegacyScriptEvidence> evidence) {
    return evidence.stream().flatMap(item -> item.arguments().stream().flatMap(argument -> versions(argument)
        .map(version -> new Candidate(version, item.script(), item.line(), item.kind())))).distinct().toList();
  }

  private static java.util.stream.Stream<String> versions(String value) {
    Matcher matcher = VERSION.matcher(value);
    var results = new java.util.ArrayList<String>();
    while (matcher.find()) results.add(matcher.group(1));
    return results.stream();
  }

  public record Candidate(String version, java.nio.file.Path script, int line, String evidenceKind) { }
}
