// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.build;

public record JavaCompatibilityPolicy(int buildJvm, int toolingJdk, int applicationTarget) {
  public JavaCompatibilityPolicy {
    if (buildJvm < 8 || toolingJdk < 8 || applicationTarget < 7) throw new IllegalArgumentException("invalid Java policy");
  }
}
