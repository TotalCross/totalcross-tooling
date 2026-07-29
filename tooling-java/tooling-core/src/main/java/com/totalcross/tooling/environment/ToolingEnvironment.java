// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.environment;

import com.totalcross.tooling.jdk.JdkInstallation;
import java.nio.file.Path;

/** Immutable, capability-proven environment consumed by CLI and build adapters. */
public record ToolingEnvironment(Path sdkHome, String sdkVersion, String sdkProvenance,
    JdkInstallation toolingJdk, int applicationJavaTarget, boolean requiresRetrolambda) {
}
