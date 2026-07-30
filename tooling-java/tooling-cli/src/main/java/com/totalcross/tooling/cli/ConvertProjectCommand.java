/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.cli;

import com.totalcross.tooling.conversion.inference.ProjectConversionAnalyzer;
import com.totalcross.tooling.conversion.plan.ConversionPlan;
import com.totalcross.tooling.conversion.plan.ConversionPlanCodec;
import com.totalcross.tooling.conversion.transaction.ProjectConversionTransaction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** CLI bootstrap entry point for conversion analysis before a project has a Gradle build. */
final class ConvertProjectCommand {
  private ConvertProjectCommand() { }

  static void execute(String[] args) throws Exception {
    if (args.length < 2) throw new IllegalArgumentException(usage());
    if ("rollback".equals(args[1])) {
      Path journal = requiredPath(args, "--journal");
      int moved = new ProjectConversionTransaction().rollback(journal);
      System.out.println("{\"schemaVersion\":1,\"event\":\"conversion-rolled-back\",\"journal\":" + quote(journal)
          + ",\"restoredFiles\":" + moved + "}");
      return;
    }
    if ("apply".equals(args[1])) {
      ProjectConversionTransaction.Result result = apply(requiredPath(args, "--plan"));
      System.out.println("{\"schemaVersion\":1,\"event\":\"conversion-applied\",\"backup\":"
          + quote(result.backup()) + ",\"journal\":" + quote(result.journal()) + ",\"movedFiles\":" + result.movedFiles() + "}");
      return;
    }
    if (!"analyze".equals(args[1])) throw new IllegalArgumentException(usage());
    Path project = requiredPath(args, "--project");
    ConversionPlan plan = analyze(project);
    String json = new ConversionPlanCodec().toJson(plan);
    Path planFile = optionalPath(args, "--plan");
    if (planFile != null) {
      if (planFile.startsWith(project)) {
        throw new IllegalArgumentException("conversion plan output must be outside the project so analysis stays read-only");
      }
      Path parent = planFile.getParent();
      if (parent != null) Files.createDirectories(parent);
      Files.writeString(planFile, json + System.lineSeparator());
    }
    System.out.println("{\"schemaVersion\":1,\"event\":\"conversion-plan\",\"plan\":" + json + "}");
  }

  static ConversionPlan analyze(Path project) throws Exception {
    return new ProjectConversionAnalyzer().analyze(project);
  }

  static ProjectConversionTransaction.Result apply(Path planFile) throws Exception {
    String contents = Files.readString(planFile);
    Path project = Path.of(stringField(contents, "project")).toAbsolutePath().normalize();
    String fingerprint = stringField(contents, "inventoryFingerprint");
    ConversionPlan current = analyze(project);
    if (!fingerprint.equals(current.inventoryFingerprint())) {
      throw new IllegalArgumentException("saved conversion plan does not match the current project; analyze again before apply");
    }
    return new ProjectConversionTransaction().apply(current);
  }

  private static Path requiredPath(String[] args, String option) {
    Path value = optionalPath(args, option);
    if (value == null) throw new IllegalArgumentException("convert-project requires " + option + " <path>");
    return value;
  }

  private static Path optionalPath(String[] args, String option) {
    for (int index = 2; index + 1 < args.length; index++) {
      if (option.equals(args[index])) return Path.of(args[index + 1]).toAbsolutePath().normalize();
    }
    return null;
  }

  private static String stringField(String json, String field) {
    Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"").matcher(json);
    if (!matcher.find()) throw new IllegalArgumentException("conversion plan is missing " + field);
    return matcher.group(1).replace("\\\\\"", "\"").replace("\\\\\\\\", "\\");
  }

  private static String quote(Path value) { return "\"" + value.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }

  private static String usage() { return "usage: convert-project analyze --project <path> [--plan <file>] | convert-project apply --plan <file> | convert-project rollback --journal <file>"; }
}
