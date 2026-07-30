/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.cli;

import com.totalcross.tooling.conversion.inference.ProjectConversionAnalyzer;
import com.totalcross.tooling.conversion.plan.ConversionPlan;
import com.totalcross.tooling.conversion.plan.ConversionPlanCodec;
import java.nio.file.Files;
import java.nio.file.Path;

/** CLI bootstrap entry point for conversion analysis before a project has a Gradle build. */
final class ConvertProjectCommand {
  private ConvertProjectCommand() { }

  static void execute(String[] args) throws Exception {
    if (args.length < 2 || !"analyze".equals(args[1])) {
      throw new IllegalArgumentException("usage: convert-project analyze --project <path> [--plan <file>]");
    }
    Path project = requiredPath(args, "--project");
    ConversionPlan plan = analyze(project);
    String json = new ConversionPlanCodec().toJson(plan);
    Path planFile = optionalPath(args, "--plan");
    if (planFile != null) {
      Path parent = planFile.getParent();
      if (parent != null) Files.createDirectories(parent);
      Files.writeString(planFile, json + System.lineSeparator());
    }
    System.out.println("{\"schemaVersion\":1,\"event\":\"conversion-plan\",\"plan\":" + json + "}");
  }

  static ConversionPlan analyze(Path project) throws Exception {
    return new ProjectConversionAnalyzer().analyze(project);
  }

  private static Path requiredPath(String[] args, String option) {
    Path value = optionalPath(args, option);
    if (value == null) throw new IllegalArgumentException("convert-project analyze requires " + option + " <path>");
    return value;
  }

  private static Path optionalPath(String[] args, String option) {
    for (int index = 2; index + 1 < args.length; index++) {
      if (option.equals(args[index])) return Path.of(args[index + 1]).toAbsolutePath().normalize();
    }
    return null;
  }
}
