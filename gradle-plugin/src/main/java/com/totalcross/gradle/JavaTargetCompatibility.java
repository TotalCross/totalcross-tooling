/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.gradle;

import java.io.IOException;
import java.nio.file.Path;

import com.totalcross.tooling.compatibility.JavaCompatibilityPolicy;
import org.gradle.api.GradleException;

/** Applies the TotalCross SDK bytecode compatibility policy. */
final class JavaTargetCompatibility {
    private JavaTargetCompatibility() {
    }

    static int targetVersion(Path jar) throws IOException {
        return JavaCompatibilityPolicy.targetVersion(jar);
    }

    static boolean requiresRetrolambda(String sdkVersion, int targetVersion) {
        try {
            return JavaCompatibilityPolicy.requiresRetrolambda(sdkVersion, targetVersion);
        } catch (IllegalArgumentException error) {
            throw new GradleException(error.getMessage(), error);
        }
    }

    static boolean usesJdk11(String sdkVersion) {
        try {
            return JavaCompatibilityPolicy.usesJdk11(sdkVersion);
        } catch (IllegalArgumentException error) {
            throw new GradleException(error.getMessage(), error);
        }
    }

    static void validate(String sdkVersion, int targetVersion) {
        try {
            JavaCompatibilityPolicy.validate(sdkVersion, targetVersion);
        } catch (IllegalArgumentException error) {
            throw new GradleException(error.getMessage(), error);
        }
    }
}
