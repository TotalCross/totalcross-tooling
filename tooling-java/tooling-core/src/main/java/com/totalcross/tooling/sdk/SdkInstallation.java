// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.sdk;

import com.totalcross.tooling.store.InstalledArtifact;
import java.nio.file.Path;

public record SdkInstallation(InstalledArtifact artifact) {
    public Path home() {
        return artifact.path();
    }
}
