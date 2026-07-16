/*
 * SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;

class DeployLogLevelTest {
    @Test
    void usesLogLevelForSdk730AndNewer() {
        assertEquals(List.of("/log-level", "debug"), TotalCrossPackageTask.logLevelArguments("7.3.0", "debug"));
        assertEquals(List.of("/log-level", "quiet"), TotalCrossPackageTask.logLevelArguments("8.0.0", "QUIET"));
    }

    @Test
    void usesVerboseFlagOnlyForLegacySdk() {
        assertEquals(List.of("/v"), TotalCrossPackageTask.logLevelArguments("7.2.2", "verbose"));
        assertEquals(List.of(), TotalCrossPackageTask.logLevelArguments("7.2.2", "debug"));
        assertEquals(List.of(), TotalCrossPackageTask.logLevelArguments("7.2.2", "normal"));
    }

    @Test
    void rejectsUnknownLogLevel() {
        assertThrows(GradleException.class, () -> TotalCrossPackageTask.logLevelArguments("7.3.0", "trace"));
    }
}
