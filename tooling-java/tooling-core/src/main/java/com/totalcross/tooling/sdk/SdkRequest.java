// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.sdk;

import com.totalcross.tooling.platform.HostPlatform;
import com.totalcross.tooling.store.ArtifactCoordinate;
import java.net.URI;
import java.nio.file.Path;

public record SdkRequest(ArtifactCoordinate coordinate, Path archive, URI source, String sha256,
                         HostPlatform hostPlatform) {
}
