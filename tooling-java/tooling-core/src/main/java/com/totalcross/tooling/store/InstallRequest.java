// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.store;

import com.totalcross.tooling.platform.HostPlatform;
import java.net.URI;
import java.nio.file.Path;

/** Inputs for a checksum-verified, atomic artifact installation. */
public record InstallRequest(ArtifactCoordinate coordinate, Path archive, URI source, String sha256,
                             HostPlatform hostPlatform) {
    public InstallRequest {
        if (archive == null || source == null || sha256 == null || sha256.length() != 64 || hostPlatform == null) {
            throw new IllegalArgumentException("archive, source, SHA-256, and host platform are required");
        }
    }
}
