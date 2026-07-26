// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import com.totalcross.tooling.platform.HostPlatform;
import java.net.URI;
import java.nio.file.Path;

public final class CorrettoProvider implements JdkProvider {
    @Override public JdkCandidate candidate(String version, String build, HostPlatform platform, Path home) {
        return new JdkCandidate("Corretto", version, build, home,
                URI.create("https://corretto.aws/downloads/resources/" + version + "/amazon-corretto-" + version + "-" + platform.id() + ".tar.gz"),
                "", false);
    }
}
