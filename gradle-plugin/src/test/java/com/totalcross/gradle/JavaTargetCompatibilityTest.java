/*
 * SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.gradle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;

class JavaTargetCompatibilityTest {
    @Test
    void acceptsSdk730ThroughJava17() {
        assertDoesNotThrow(() -> JavaTargetCompatibility.validate("7.3.0", 17));
        assertDoesNotThrow(() -> JavaTargetCompatibility.validate("8.0.0", 17));
        assertFalse(JavaTargetCompatibility.requiresRetrolambda("7.3.0", 8));
    }

    @Test
    void rejectsSdk730AboveJava17() {
        assertThrows(GradleException.class, () -> JavaTargetCompatibility.validate("7.3.0", 18));
    }

    @Test
    void rejectsLegacySdkAboveJava8() {
        assertThrows(GradleException.class, () -> JavaTargetCompatibility.validate("7.2.2", 9));
        assertThrows(GradleException.class, () -> JavaTargetCompatibility.validate("6.9.9", 17));
    }

    @Test
    void lowersOnlyLegacyJava8() {
        assertTrue(JavaTargetCompatibility.requiresRetrolambda("7.2.2", 8));
        assertFalse(JavaTargetCompatibility.requiresRetrolambda("7.2.2", 7));
        assertTrue(JavaTargetCompatibility.usesJdk11("7.2.2"));
        assertFalse(JavaTargetCompatibility.usesJdk11("7.3.0"));
    }
}
