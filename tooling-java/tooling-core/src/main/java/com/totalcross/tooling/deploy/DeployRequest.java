// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.deploy;

import java.nio.file.Path;
import java.util.List;

/** Immutable input for a deploy operation; it does not expose DeploySettings. */
public record DeployRequest(Path inputArtifact, Path outputDirectory, Path sdkInstallation, Path toolingJdk,
                            List<DeployPlatform> platforms, boolean library, DeployLogLevel logLevel,
                            List<String> legacyOptions) {
    public DeployRequest {
        if (inputArtifact == null || outputDirectory == null || sdkInstallation == null || toolingJdk == null) {
            throw new IllegalArgumentException("input, output, SDK, and JDK paths are required");
        }
        platforms = List.copyOf(platforms == null ? List.of() : platforms);
        legacyOptions = List.copyOf(legacyOptions == null ? List.of() : legacyOptions);
        logLevel = logLevel == null ? DeployLogLevel.NORMAL : logLevel;
    }
}
