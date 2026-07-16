/*
 * SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.gradle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.gradle.api.GradleException;

/** Applies the TotalCross SDK bytecode compatibility policy. */
final class JavaTargetCompatibility {
    private JavaTargetCompatibility() {
    }

    static int targetVersion(Path jar) throws IOException {
        int highest = 0;
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;
                try (InputStream input = zip.getInputStream(entry)) {
                    byte[] header = input.readNBytes(8);
                    if (header.length != 8 || header[0] != (byte) 0xCA || header[1] != (byte) 0xFE
                            || header[2] != (byte) 0xBA || header[3] != (byte) 0xBE) {
                        throw new IOException("Invalid class file in application JAR: " + entry.getName());
                    }
                    int major = ((header[6] & 0xff) << 8) | (header[7] & 0xff);
                    highest = Math.max(highest, major - 44);
                }
            }
        }
        if (highest == 0) throw new IOException("Application JAR contains no class files: " + jar);
        return highest;
    }

    static boolean requiresRetrolambda(String sdkVersion, int targetVersion) {
        validate(sdkVersion, targetVersion);
        return isBefore730(sdkVersion) && targetVersion == 8;
    }

    static boolean usesJdk11(String sdkVersion) {
        return isBefore730(sdkVersion);
    }

    static void validate(String sdkVersion, int targetVersion) {
        if (isBefore730(sdkVersion) && targetVersion > 8) {
            throw new GradleException("TotalCross SDK " + sdkVersion + " supports at most Java target 8; found target " + targetVersion + ".");
        }
        if (!isBefore730(sdkVersion) && targetVersion > 17) {
            throw new GradleException("TotalCross SDK " + sdkVersion + " supports at most Java target 17; found target " + targetVersion + ".");
        }
    }

    private static boolean isBefore730(String version) {
        String[] parts = version.split("[.-]", 4);
        if (parts.length < 3) throw new GradleException("TotalCross SDK version must use major.minor.patch format: " + version);
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = Integer.parseInt(parts[2]);
            return major < 7 || (major == 7 && minor < 3);
        } catch (NumberFormatException error) {
            throw new GradleException("TotalCross SDK version must use numeric major.minor.patch format: " + version, error);
        }
    }
}
