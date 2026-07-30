/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.inference;

import com.totalcross.tooling.compatibility.JavaCompatibilityPolicy;
import com.totalcross.tooling.conversion.script.LegacyScriptEvidence;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;

/** Chooses a Java application target from explicit compiler evidence before SDK defaults. */
public final class JavaTargetInference {
  public Result infer(String sdkVersion, List<LegacyScriptEvidence> evidence) {
    return resolve(sdkVersion, compilerTarget(evidence), OptionalInt.empty(), OptionalInt.empty());
  }

  /** Uses compiled output only after the explicit compiler flags have been considered. */
  public Result infer(String sdkVersion, List<LegacyScriptEvidence> evidence, Path project) throws IOException {
    OptionalInt compiler = compilerTarget(evidence);
    if (compiler.isPresent()) return resolve(sdkVersion, compiler, OptionalInt.empty(), OptionalInt.empty());
    OptionalInt build = new ExistingBuildTargetInference().infer(project);
    if (build.isPresent()) return resolve(sdkVersion, compiler, build, OptionalInt.empty());
    return resolve(sdkVersion, compiler, build, new ClassfileTargetInference().infer(project));
  }

  private static OptionalInt compilerTarget(List<LegacyScriptEvidence> evidence) {
    int[] releases = targets(evidence, JavaTargetInference::release);
    int[] sourceTargets = targets(evidence, JavaTargetInference::sourceTarget);
    if (releases.length > 1 || sourceTargets.length > 1 || (releases.length == 1 && sourceTargets.length == 1 && releases[0] != sourceTargets[0])) {
      throw new IllegalArgumentException("conflicting Java compiler targets require explicit selection");
    }
    return releases.length == 1 ? OptionalInt.of(releases[0])
        : sourceTargets.length == 1 ? OptionalInt.of(sourceTargets[0]) : OptionalInt.empty();
  }

  private static Result resolve(String sdkVersion, OptionalInt compilerTarget, OptionalInt buildTarget, OptionalInt classfileTarget) {
    int target = compilerTarget.orElseGet(() -> buildTarget.orElseGet(
        () -> classfileTarget.orElseGet(() -> JavaCompatibilityPolicy.highestApplicationTarget(sdkVersion))));
    JavaCompatibilityPolicy.validate(sdkVersion, target);
    String source = compilerTarget.isPresent() ? "javac compiler target" : buildTarget.isPresent() ? "existing build target"
        : classfileTarget.isPresent() ? "compiled class-file target" : "SDK compatibility default";
    return new Result(target, source);
  }

  private static OptionalInt release(List<String> arguments) {
    for (int index = 0; index + 1 < arguments.size(); index++) {
      if ("--release".equals(arguments.get(index))) {
        try { return OptionalInt.of(Integer.parseInt(arguments.get(index + 1).replaceFirst("^1\\.", ""))); }
        catch (NumberFormatException ignored) { return OptionalInt.empty(); }
      }
    }
    return OptionalInt.empty();
  }

  private static int[] targets(List<LegacyScriptEvidence> evidence, java.util.function.Function<List<String>, OptionalInt> finder) {
    return evidence.stream().filter(item -> item.kind().equals("javac")).map(LegacyScriptEvidence::arguments).map(finder)
        .filter(OptionalInt::isPresent).mapToInt(OptionalInt::getAsInt).distinct().toArray();
  }

  private static OptionalInt sourceTarget(List<String> arguments) {
    OptionalInt source = option(arguments, "-source"), target = option(arguments, "-target");
    return source.isPresent() && target.isPresent() && source.getAsInt() == target.getAsInt() ? source : OptionalInt.empty();
  }

  private static OptionalInt option(List<String> arguments, String option) {
    for (int index = 0; index + 1 < arguments.size(); index++) if (option.equals(arguments.get(index))) {
      try { return OptionalInt.of(Integer.parseInt(arguments.get(index + 1).replaceFirst("^1\\.", ""))); }
      catch (NumberFormatException ignored) { return OptionalInt.empty(); }
    }
    return OptionalInt.empty();
  }

  public record Result(int target, String source) { }
}
