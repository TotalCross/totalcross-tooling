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
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads literal Java target declarations from an existing root Gradle or Maven build. */
final class ExistingBuildTargetInference {
  private static final List<String> BUILDS = List.of("build.gradle", "build.gradle.kts", "pom.xml");
  private static final Pattern RELEASE = Pattern.compile("(?:options\\.release\\s*=|<maven\\.compiler\\.release>)\\s*(?:['\\\"])?(?:1\\.)?(\\d+)");
  private static final Pattern TOOLCHAIN = Pattern.compile("JavaLanguageVersion\\.of\\(\\s*(\\d+)\\s*\\)");
  private static final Pattern SOURCE = Pattern.compile("(?:sourceCompatibility\\s*=|<maven\\.compiler\\.source>)\\s*(?:JavaVersion\\.VERSION_)?(?:['\\\"])?(?:1[._])?(\\d+)");
  private static final Pattern TARGET = Pattern.compile("(?:targetCompatibility\\s*=|<maven\\.compiler\\.target>)\\s*(?:JavaVersion\\.VERSION_)?(?:['\\\"])?(?:1[._])?(\\d+)");

  OptionalInt infer(Path project) throws IOException {
    List<Integer> targets = new ArrayList<>();
    for (String name : BUILDS) {
      Path build = project.resolve(name);
      if (!Files.isRegularFile(build)) continue;
      String text = Files.readString(build);
      targets.addAll(values(RELEASE, text));
      targets.addAll(values(TOOLCHAIN, text));
      List<Integer> sources = values(SOURCE, text), configuredTargets = values(TARGET, text);
      if (sources.size() == 1 && configuredTargets.size() == 1 && sources.get(0).equals(configuredTargets.get(0))) {
        targets.add(sources.get(0));
      }
    }
    int[] distinct = targets.stream().mapToInt(Integer::intValue).distinct().toArray();
    if (distinct.length > 1) throw new IllegalArgumentException("conflicting existing build Java targets require explicit selection");
    return distinct.length == 1 ? OptionalInt.of(distinct[0]) : OptionalInt.empty();
  }

  private static List<Integer> values(Pattern pattern, String text) {
    Matcher matcher = pattern.matcher(text);
    List<Integer> values = new ArrayList<>();
    while (matcher.find()) values.add(Integer.parseInt(matcher.group(1)));
    return values;
  }
}
