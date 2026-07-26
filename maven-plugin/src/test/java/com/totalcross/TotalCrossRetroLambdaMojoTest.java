/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */

package com.totalcross;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;

class TotalCrossRetroLambdaMojoTest {
    @Test
    void detectsModernSdkBytecode() throws Exception {
        Path root = Files.createTempDirectory("totalcross-retrolambda-");
        try {
            Path sdk = root.resolve("totalcross-sdk.jar");
            writeClass(sdk, 61);
            assertTrue(TotalCrossRetroLambdaMojo.hasModernBytecode(sdk.toFile()));

            Path legacy = root.resolve("totalcross-sdk-legacy.jar");
            writeClass(legacy, 55);
            assertFalse(TotalCrossRetroLambdaMojo.hasModernBytecode(legacy.toFile()));
        } finally {
            FileUtils.deleteDirectory(root.toFile());
        }
    }

    private static void writeClass(Path jarPath, int majorVersion) throws Exception {
        try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(jarPath.toFile()))) {
            output.putNextEntry(new ZipEntry("totalcross/ui/Container.class"));
            output.write(new byte[] {(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe, 0, 0,
                (byte) (majorVersion >> 8), (byte) majorVersion});
            output.closeEntry();
        }
    }
}
