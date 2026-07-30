/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.inference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads a literal SDK version from TotalCross project metadata without interpreting arbitrary JSON. */
final class ProjectMetadataSdkInference {
  private static final Pattern SDK_VERSION = Pattern.compile("\\\"sdkVersion\\\"\\s*:\\s*\\\"([0-9]+\\.[0-9]+\\.[0-9]+)\\\"");

  List<SdkVersionInference.Candidate> infer(Path project) throws IOException {
    Path metadata = project.resolve(".totalcross/project.json");
    if (!Files.isRegularFile(metadata)) return List.of();
    String text = Files.readString(metadata);
    Matcher matcher = SDK_VERSION.matcher(text);
    if (!matcher.find()) return List.of();
    return List.of(new SdkVersionInference.Candidate(matcher.group(1), project.relativize(metadata),
        (int) text.substring(0, matcher.start(1)).chars().filter(character -> character == '\n').count() + 1, "project-metadata"));
  }
}
