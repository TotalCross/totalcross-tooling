// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.download;

import java.net.URI;
import java.nio.file.Path;

/** A concrete download request; callers must supply a concrete source and digest. */
public record DownloadRequest(URI source, Path destination, String sha256) {
    public DownloadRequest {
        if (source == null || destination == null || sha256 == null || sha256.length() != 64) {
            throw new IllegalArgumentException("source, destination, and SHA-256 are required");
        }
    }
}
