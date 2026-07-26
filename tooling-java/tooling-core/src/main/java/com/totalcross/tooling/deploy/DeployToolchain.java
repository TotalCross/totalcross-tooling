// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.deploy;

import java.nio.file.Path;
import java.util.List;

/** Narrow classpath supplied to the isolated legacy deploy adapter. */
public record DeployToolchain(List<Path> artifacts) {
    public DeployToolchain {
        artifacts = List.copyOf(artifacts);
        if (artifacts.isEmpty()) throw new IllegalArgumentException("At least one deploy artifact is required");
    }
}
