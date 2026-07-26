// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Selects the first candidate proven capable of running the required tools. */
public final class JdkSelector {
    private final JdkCapabilityProbe probe;

    public JdkSelector(JdkCapabilityProbe probe) {
        this.probe = probe;
    }

    public JdkInstallation select(JdkRequest request, List<JdkCandidate> candidates) throws JdkSelectionException {
        if (request.explicitPath() != null) {
            JdkInstallation explicit = new JdkInstallation("explicit", request.version(), "user", request.explicitPath());
            JdkCapabilityReport result = probe.probe(explicit, request.protoc());
            if (result.accepted()) return explicit;
            throw new JdkSelectionException(result.failures());
        }
        List<String> diagnostics = new ArrayList<>();
        for (JdkCandidate candidate : candidates) {
            if (candidate.crac()) {
                diagnostics.add(candidate.vendor() + " " + candidate.version() + " rejected: CRaC build");
                continue;
            }
            JdkCapabilityReport result = probe.probe(candidate.installation(), request.protoc());
            if (result.accepted()) return candidate.installation();
            diagnostics.add(candidate.vendor() + " " + candidate.version() + ": " + String.join(", ", result.failures()));
        }
        throw new JdkSelectionException(diagnostics);
    }
}
