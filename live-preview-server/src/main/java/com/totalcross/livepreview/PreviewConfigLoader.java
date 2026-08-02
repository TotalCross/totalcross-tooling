// Copyright (C) 2026 Amalgam Solucoes em TI Ltda
//
// SPDX-License-Identifier: LGPL-2.1-only
package com.totalcross.livepreview;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads and writes the lightweight JSON config consumed by PreviewRunner.
 */
public final class PreviewConfigLoader {
  public static final String DEFAULT_FILE_NAME = "totalcross-preview.json";

  private PreviewConfigLoader() {
  }

  public static PreviewConfig load(Path configPath) throws IOException {
    if (!Files.exists(configPath)) {
      return PreviewConfig.defaults();
    }
    return parse(new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8));
  }

  public static PreviewConfig createDefault(Path configPath) throws IOException {
    PreviewConfig config = PreviewConfig.defaults();
    Path parent = configPath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.write(configPath, toJson(config).getBytes(StandardCharsets.UTF_8));
    return config;
  }

  public static PreviewConfig parse(String json) {
    PreviewConfig config = PreviewConfig.defaults();
    config.mainWindow = stringValue(json, "mainWindow", config.mainWindow);
    config.launcherArgs = stringArray(json, "launcherArgs", config.launcherArgs);
    config.buildCommand = stringValue(json, "buildCommand", config.buildCommand);
    config.classOutputPaths = stringArray(json, "classOutputPaths", config.classOutputPaths);
    config.resourcePaths = stringArray(json, "resourcePaths", config.resourcePaths);
    config.dependencyPaths = stringArray(json, "dependencyPaths", config.dependencyPaths);
    config.previewMode = stringValue(json, "previewMode", config.previewMode);
    config.reloadMode = stringValue(json, "reloadMode", config.reloadMode);
    config.width = intValue(json, "width", config.width);
    config.height = intValue(json, "height", config.height);
    config.scale = intValue(json, "scale", config.scale);
    config.platform = stringValue(json, "platform", config.platform);
    config.headlessOutput = stringValue(json, "headlessOutput", config.headlessOutput);
    return config;
  }

  public static String toJson(PreviewConfig config) {
    StringBuilder json = new StringBuilder();
    json.append("{\n");
    appendString(json, "mainWindow", config.mainWindow, true);
    appendArray(json, "launcherArgs", config.launcherArgs, true);
    appendString(json, "buildCommand", config.buildCommand, true);
    appendArray(json, "classOutputPaths", config.classOutputPaths, true);
    appendArray(json, "resourcePaths", config.resourcePaths, true);
    appendArray(json, "dependencyPaths", config.dependencyPaths, true);
    appendString(json, "previewMode", config.previewMode, true);
    appendString(json, "reloadMode", config.reloadMode, true);
    appendNumber(json, "width", config.width, true);
    appendNumber(json, "height", config.height, true);
    appendNumber(json, "scale", config.scale, true);
    appendString(json, "platform", config.platform, true);
    appendString(json, "headlessOutput", config.headlessOutput, false);
    json.append("}\n");
    return json.toString();
  }

  private static String stringValue(String json, String key, String defaultValue) {
    Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"").matcher(json);
    return matcher.find() ? unescape(matcher.group(1)) : defaultValue;
  }

  private static int intValue(String json, String key, int defaultValue) {
    Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)").matcher(json);
    return matcher.find() ? Integer.parseInt(matcher.group(1)) : defaultValue;
  }

  private static List<String> stringArray(String json, String key, List<String> defaultValue) {
    Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL)
        .matcher(json);
    if (!matcher.find()) {
      return defaultValue;
    }
    List<String> values = new ArrayList<>();
    Matcher itemMatcher = Pattern.compile("\"((?:\\\\.|[^\"])*)\"").matcher(matcher.group(1));
    while (itemMatcher.find()) {
      values.add(unescape(itemMatcher.group(1)));
    }
    return values;
  }

  private static void appendString(StringBuilder json, String key, String value, boolean comma) {
    json.append("  \"").append(key).append("\": \"").append(escape(value)).append("\"");
    appendComma(json, comma);
  }

  private static void appendNumber(StringBuilder json, String key, int value, boolean comma) {
    json.append("  \"").append(key).append("\": ").append(value);
    appendComma(json, comma);
  }

  private static void appendArray(StringBuilder json, String key, List<String> values, boolean comma) {
    json.append("  \"").append(key).append("\": [");
    for (int i = 0; values != null && i < values.size(); i++) {
      if (i > 0) {
        json.append(", ");
      }
      json.append("\"").append(escape(values.get(i))).append("\"");
    }
    json.append("]");
    appendComma(json, comma);
  }

  private static void appendComma(StringBuilder json, boolean comma) {
    if (comma) {
      json.append(",");
    }
    json.append("\n");
  }

  private static String escape(String value) {
    return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private static String unescape(String value) {
    StringBuilder result = new StringBuilder();
    boolean escape = false;
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (escape) {
        result.append(c);
        escape = false;
      } else if (c == '\\') {
        escape = true;
      } else {
        result.append(c);
      }
    }
    return result.toString();
  }
}
