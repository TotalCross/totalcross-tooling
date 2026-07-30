/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.compatibility;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JavaCompatibilityPolicyTest {
    @Test
    void appliesSdkGenerationPolicy() {
        JavaCompatibilityPolicy.validate("7.2.2", 8);
        JavaCompatibilityPolicy.validate("7.3.0", 17);
        assertTrue(JavaCompatibilityPolicy.requiresRetrolambda("7.2.2", 8));
        assertFalse(JavaCompatibilityPolicy.requiresRetrolambda("7.3.0", 8));
        assertTrue(JavaCompatibilityPolicy.usesJdk11("7.2.2"));
        assertFalse(JavaCompatibilityPolicy.usesJdk11("7.3.0"));
        assertEquals(8, JavaCompatibilityPolicy.highestApplicationTarget("7.2.2"));
        assertEquals(17, JavaCompatibilityPolicy.highestApplicationTarget("7.3.0"));
    }

    @Test
    void rejectsUnsupportedApplicationTargets() {
        assertThrows(IllegalArgumentException.class,
                () -> JavaCompatibilityPolicy.validate("7.2.2", 9));
        assertThrows(IllegalArgumentException.class,
                () -> JavaCompatibilityPolicy.validate("7.3.0", 18));
    }
}
