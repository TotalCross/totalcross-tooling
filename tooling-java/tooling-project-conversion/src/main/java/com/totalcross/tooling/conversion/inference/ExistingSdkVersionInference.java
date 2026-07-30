/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.inference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads literal TotalCross SDK coordinates from root Gradle and Maven build files. */
final class ExistingSdkVersionInference {
  private static final Pattern GRADLE = Pattern.compile("com\\.totalcross:totalcross-sdk:([0-9]+\\.[0-9]+\\.[0-9]+)");
  private static final Pattern MAVEN = Pattern.compile("(?s)<dependency>.*?<groupId>com\\.totalcross</groupId>.*?<artifactId>totalcross-sdk</artifactId>.*?<version>\\s*([0-9]+\\.[0-9]+\\.[0-9]+)\\s*</version>.*?</dependency>");

  List<SdkVersionInference.Candidate> infer(Path project) throws IOException {
    List<SdkVersionInference.Candidate> candidates = new ArrayList<>();
    collect(candidates, project.resolve("build.gradle"), GRADLE, "existing-gradle");
    collect(candidates, project.resolve("build.gradle.kts"), GRADLE, "existing-gradle");
    collect(candidates, project.resolve("pom.xml"), MAVEN, "existing-maven");
    return candidates.stream().distinct().toList();
  }

  private static void collect(List<SdkVersionInference.Candidate> candidates, Path file, Pattern pattern, String kind) throws IOException {
    if (!Files.isRegularFile(file)) return;
    String text = Files.readString(file);
    Matcher matcher = pattern.matcher(text);
    while (matcher.find()) candidates.add(new SdkVersionInference.Candidate(matcher.group(1), file.getFileName(), line(text, matcher.start(1)), kind));
  }

  private static int line(String text, int index) { return (int) text.substring(0, index).chars().filter(character -> character == '\n').count() + 1; }
}
