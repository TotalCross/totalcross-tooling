// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.process;

public record ProcessResult(int exitCode, String stdout, String stderr, boolean timedOut) {
    public boolean succeeded() {
        return !timedOut && exitCode == 0;
    }
}
