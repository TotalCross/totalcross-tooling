// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import java.net.URI;
import java.nio.file.Path;

public record JdkCandidate(String vendor, String version, String build, Path home, URI source, String sha256,
                           boolean crac) {
    public JdkInstallation installation() {
        return new JdkInstallation(vendor, version, build, home);
    }
}
