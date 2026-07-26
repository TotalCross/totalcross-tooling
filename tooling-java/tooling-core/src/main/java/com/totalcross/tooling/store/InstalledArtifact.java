// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.store;

import java.nio.file.Path;

/** Completed installation and the exact archive digest used to create it. */
public record InstalledArtifact(ArtifactCoordinate coordinate, Path path, String sha256) {
}
