// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import java.util.List;

public record JdkCapabilityReport(JdkInstallation installation, List<String> failures) {
    public JdkCapabilityReport {
        failures = List.copyOf(failures);
    }

    public boolean accepted() {
        return failures.isEmpty();
    }
}
