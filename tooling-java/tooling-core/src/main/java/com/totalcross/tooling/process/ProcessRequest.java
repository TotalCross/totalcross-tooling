// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.process;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public record ProcessRequest(List<String> command, Path workingDirectory, Map<String, String> environment,
                             Duration timeout) {
    public ProcessRequest {
        if (command == null || command.isEmpty() || command.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("A non-empty process command is required");
        }
        timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
        environment = environment == null ? Map.of() : Map.copyOf(environment);
    }
}
