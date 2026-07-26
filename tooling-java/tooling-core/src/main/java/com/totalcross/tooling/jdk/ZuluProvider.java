// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import com.totalcross.tooling.platform.HostPlatform;
import java.net.URI;
import java.nio.file.Path;

public final class ZuluProvider implements JdkProvider {
    @Override public JdkCandidate candidate(String version, String build, HostPlatform platform, Path home) {
        if (build.toLowerCase().contains("crac")) throw new IllegalArgumentException("CRaC Zulu builds are not supported");
        return new JdkCandidate("Zulu", version, build, home,
                URI.create("https://cdn.azul.com/zulu/bin/zulu" + version + "-" + build + "-" + platform.id() + ".tar.gz"),
                "", false);
    }
}
