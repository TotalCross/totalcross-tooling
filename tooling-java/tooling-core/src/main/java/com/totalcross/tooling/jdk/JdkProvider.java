// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import com.totalcross.tooling.platform.HostPlatform;
import java.nio.file.Path;

public interface JdkProvider {
    JdkCandidate candidate(String version, String build, HostPlatform platform, Path home);
}
