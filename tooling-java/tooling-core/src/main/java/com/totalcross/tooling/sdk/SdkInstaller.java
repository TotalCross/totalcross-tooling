// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.sdk;

import com.totalcross.tooling.store.ArtifactInstaller;

public final class SdkInstaller {
    private final ArtifactInstaller installer;

    public SdkInstaller(ArtifactInstaller installer) {
        this.installer = installer;
    }

    public SdkInstallation install(SdkRequest request) throws java.io.IOException {
        return new SdkInstallation(installer.install(new com.totalcross.tooling.store.InstallRequest(
                request.coordinate(), request.archive(), request.source(), request.sha256(), request.hostPlatform())));
    }
}
