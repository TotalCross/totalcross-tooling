// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import java.util.List;

public final class JdkSelectionException extends Exception {
    private final List<String> diagnostics;

    public JdkSelectionException(List<String> diagnostics) {
        super("No usable JDK candidate: " + String.join("; ", diagnostics));
        this.diagnostics = List.copyOf(diagnostics);
    }

    public List<String> diagnostics() {
        return diagnostics;
    }
}
