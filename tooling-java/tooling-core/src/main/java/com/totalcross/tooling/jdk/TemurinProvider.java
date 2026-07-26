// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import com.totalcross.tooling.platform.HostPlatform;
import java.net.URI;
import java.nio.file.Path;

public final class TemurinProvider implements JdkProvider {
    @Override public JdkCandidate candidate(String version, String build, HostPlatform platform, Path home) {
        return new JdkCandidate("Temurin", version, build, home,
                URI.create("https://github.com/adoptium/temurin" + version + "-binaries/releases/download/" + build
                        + "/OpenJDK" + version + "U-jdk_" + platform.id() + "_" + build + ".tar.gz"),
                "", false);
    }
}
