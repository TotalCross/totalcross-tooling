// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.deploy;

import java.nio.file.Path;
import java.util.List;

public record DeployResult(int exitCode, List<Path> outputs, List<DeployDiagnostic> diagnostics) {
    public DeployResult {
        outputs = List.copyOf(outputs);
        diagnostics = List.copyOf(diagnostics);
    }

    public boolean succeeded() { return exitCode == 0; }
}
