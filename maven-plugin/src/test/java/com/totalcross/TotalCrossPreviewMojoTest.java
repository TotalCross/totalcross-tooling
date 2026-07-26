/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */

package com.totalcross;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class TotalCrossPreviewMojoTest {
    @Test
    void discoversPackagedMainClassFromArtifactName() throws Exception {
        Path output = Files.createTempDirectory("totalcross-preview-classes-");
        try {
            Path mainClass = output.resolve("totalcross/sample/main/TCSample.class");
            Files.createDirectories(mainClass.getParent());
            Files.write(mainClass, new byte[] {0});
            assertEquals("totalcross.sample.main.TCSample", TotalCrossPreviewMojo.discoverMainClass(output, "TCSample"));
        } finally {
            delete(output);
        }
    }

    private static void delete(Path path) throws Exception {
        if (Files.isDirectory(path)) {
            try (java.util.stream.Stream<Path> children = Files.list(path)) {
                children.forEach(child -> {
                    try { delete(child); } catch (Exception e) { throw new RuntimeException(e); }
                });
            }
        }
        Files.deleteIfExists(path);
    }
}
