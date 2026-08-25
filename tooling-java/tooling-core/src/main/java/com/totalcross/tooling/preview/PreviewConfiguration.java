// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.preview;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The project-owned settings used by the authenticated preview coordinator. */
public record PreviewConfiguration(String mainWindow, List<String> launcherArgs, List<Path> classpath) {
  private static final Pattern STRING = Pattern.compile("\\\"%s\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");
  private static final Pattern ARRAY = Pattern.compile("\\\"%s\\\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);

  public PreviewConfiguration {
    mainWindow = mainWindow == null ? "" : mainWindow;
    launcherArgs = List.copyOf(launcherArgs == null ? List.of() : launcherArgs);
    classpath = List.copyOf(classpath == null ? List.of() : classpath);
  }

  public static PreviewConfiguration empty() { return new PreviewConfiguration("", List.of(), List.of()); }

  public static PreviewConfiguration read(Path file, Path project) throws IOException {
    if (file == null || !Files.isRegularFile(file)) return empty();
    String json = Files.readString(file, StandardCharsets.UTF_8);
    String mainWindow = string(json, "mainWindow");
    List<String> launcherArgs = array(json, "launcherArgs");
    LinkedHashSet<Path> entries = new LinkedHashSet<>();
    for (String key : List.of("classpath", "additionalClasspath", "classOutputPaths", "resourcePaths", "dependencyPaths")) {
      for (String value : array(json, key)) {
        if (!value.isBlank()) entries.add(resolve(project, value));
      }
    }
    return new PreviewConfiguration(mainWindow, launcherArgs, new ArrayList<>(entries));
  }

  private static Path resolve(Path project, String value) {
    Path path = Path.of(value);
    return (path.isAbsolute() ? path : project.resolve(path)).toAbsolutePath().normalize();
  }

  private static String string(String json, String key) {
    Matcher matcher = Pattern.compile(String.format(STRING.pattern(), Pattern.quote(key))).matcher(json);
    return matcher.find() ? unescape(matcher.group(1)) : "";
  }

  private static List<String> array(String json, String key) {
    Matcher matcher = Pattern.compile(String.format(ARRAY.pattern(), Pattern.quote(key))).matcher(json);
    if (!matcher.find()) return List.of();
    Matcher item = Pattern.compile("\\\"((?:\\\\.|[^\\\"])*)\\\"").matcher(matcher.group(1));
    List<String> values = new ArrayList<>();
    while (item.find()) values.add(unescape(item.group(1)));
    return values;
  }

  private static String unescape(String value) {
    return value.replace("\\\\", "\\").replace("\\\"", "\"").replace("\\n", "\n");
  }
}
