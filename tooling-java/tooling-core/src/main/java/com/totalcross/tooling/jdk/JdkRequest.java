// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import java.nio.file.Path;

public record JdkRequest(String version, Path explicitPath, Path protoc) {
    public JdkRequest {
        if (version == null || version.isBlank()) throw new IllegalArgumentException("JDK version is required");
    }
}
