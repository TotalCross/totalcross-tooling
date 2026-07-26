// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.deploy;

public record DeployDiagnostic(Severity severity, String message) {
    public enum Severity { INFO, WARNING, ERROR }
}
