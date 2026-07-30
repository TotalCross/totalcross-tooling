/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.script;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Recognizes literal supported commands without shell evaluation, sourcing, or macro expansion. */
public final class LegacyScriptAnalyzer {
  public List<LegacyScriptEvidence> analyze(Path project) throws IOException {
    List<LegacyScriptEvidence> results = new ArrayList<>();
    try (var files = Files.list(project)) {
      for (Path file : files.filter(Files::isRegularFile).toList()) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".sh") || name.endsWith(".bash") || name.endsWith(".command") || name.endsWith(".bat")
            || name.endsWith(".cmd") || name.equals("makefile")) results.addAll(analyzeFile(project.relativize(file), Files.readAllLines(file)));
      }
    }
    return results;
  }

  List<LegacyScriptEvidence> analyzeFile(Path script, List<String> lines) {
    Map<String, String> variables = new HashMap<>();
    List<LegacyScriptEvidence> results = new ArrayList<>();
    for (int index = 0; index < lines.size(); index++) {
      String line = lines.get(index).trim();
      if (line.isBlank() || line.startsWith("#") || line.regionMatches(true, 0, "rem ", 0, 4)) continue;
      if (assignment(line, variables)) continue;
      if (line.contains("$(") || line.contains("`") || line.contains("source ") || line.startsWith(".")) continue;
      String expanded = expand(line, variables);
      String kind = commandKind(expanded);
      if (kind == null) continue;
      boolean secret = expanded.toLowerCase(Locale.ROOT).matches(".*(activation[-_ ]?key|password|certificate).*");
      results.add(new LegacyScriptEvidence(script, index + 1, kind, tokenize(secret ? redact(expanded) : expanded), secret));
    }
    return results;
  }

  private static boolean assignment(String line, Map<String, String> variables) {
    String assignment = line.regionMatches(true, 0, "set ", 0, 4) ? line.substring(4).trim() : line;
    int equals = assignment.indexOf('=');
    if (equals <= 0 || assignment.substring(0, equals).contains(" ")) return false;
    String key = assignment.substring(0, equals).trim();
    if (!key.matches("[A-Za-z_][A-Za-z0-9_]*")) return false;
    variables.put(key, assignment.substring(equals + 1).trim().replaceAll("^\"|\"$", ""));
    return true;
  }

  private static String expand(String line, Map<String, String> variables) {
    String value = line;
    for (var entry : variables.entrySet()) value = value.replace("$" + entry.getKey(), entry.getValue())
        .replace("${" + entry.getKey() + "}", entry.getValue()).replace("%" + entry.getKey() + "%", entry.getValue());
    return value;
  }

  private static String commandKind(String line) {
    if (line.matches(".*\\bjavac\\b.*")) return "javac";
    if (line.matches(".*\\btotalcross\\.Launcher\\b.*")) return "launcher";
    if (line.matches(".*\\btc\\.Deploy\\b.*")) return "deploy";
    if (line.matches(".*\\bjar\\b.*")) return "jar";
    return null;
  }

  private static String redact(String line) {
    return line.replaceAll("(?i)((?:activation[-_ ]?key|password|certificate)\\s*(?:=|:)\\s*)([^\\s]+)", "$1<redacted>");
  }

  private static List<String> tokenize(String line) {
    List<String> tokens = new ArrayList<>();
    boolean quoted = false;
    StringBuilder token = new StringBuilder();
    for (char character : line.toCharArray()) {
      if (character == '\"') { quoted = !quoted; continue; }
      if (Character.isWhitespace(character) && !quoted) { if (token.length() > 0) { tokens.add(token.toString()); token.setLength(0); } }
      else token.append(character);
    }
    if (token.length() > 0) tokens.add(token.toString());
    return tokens;
  }
}
