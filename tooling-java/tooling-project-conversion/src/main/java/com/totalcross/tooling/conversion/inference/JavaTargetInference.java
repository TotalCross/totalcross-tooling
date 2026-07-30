/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.inference;

import com.totalcross.tooling.compatibility.JavaCompatibilityPolicy;
import com.totalcross.tooling.conversion.script.LegacyScriptEvidence;
import java.util.List;
import java.util.OptionalInt;

/** Chooses a Java application target from explicit compiler evidence before SDK defaults. */
public final class JavaTargetInference {
  public Result infer(String sdkVersion, List<LegacyScriptEvidence> evidence) {
    OptionalInt release = evidence.stream().filter(item -> item.kind().equals("javac"))
        .map(LegacyScriptEvidence::arguments).map(JavaTargetInference::release).filter(OptionalInt::isPresent)
        .mapToInt(OptionalInt::getAsInt).findFirst();
    int target = release.orElseGet(() -> JavaCompatibilityPolicy.highestApplicationTarget(sdkVersion));
    JavaCompatibilityPolicy.validate(sdkVersion, target);
    String source = release.isPresent() ? "javac --release" : "SDK compatibility default";
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

  public record Result(int target, String source) { }
}
