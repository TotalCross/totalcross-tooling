// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.deploy;

import java.nio.file.Path;
import java.util.List;

/** Narrow classpath supplied to the isolated legacy deploy adapter. */
public record DeployToolchain(List<Path> artifacts, Path protoc, Path bundletool) {
    public static final String PROTOC_PROPERTY = "totalcross.tooling.android.protoc";
    public static final String BUNDLETOOL_PROPERTY = "totalcross.tooling.android.bundletool";
    public DeployToolchain(List<Path> artifacts) {
        this(artifacts, null, null);
    }

    public DeployToolchain {
        artifacts = List.copyOf(artifacts);
        if (artifacts.isEmpty()) throw new IllegalArgumentException("At least one deploy artifact is required");
    }

    public DeployToolchain withAndroidTools(Path resolvedProtoc, Path resolvedBundletool) {
        return new DeployToolchain(artifacts, resolvedProtoc, resolvedBundletool);
    }
}
