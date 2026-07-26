/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */

package com.totalcross;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

class DownloadManagerTest {
    @Test
    void replacesAnInterruptedExtractionBeforeUnzipping() throws Exception {
        Path root = Files.createTempDirectory("totalcross-download-");
        try {
            String archiveRoot = "zulu11.90.19-ca-fx-jdk11.0.32-macosx_x64";
            Path partial = root.resolve(archiveRoot).resolve("Contents/Home/man/ja");
            Files.createDirectories(partial);
            Path archive = root.resolve("jdk.zip");
            try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(archive.toFile()))) {
                output.putNextEntry(new ZipEntry(archiveRoot + "/"));
                output.closeEntry();
                output.putNextEntry(new ZipEntry(archiveRoot + "/Contents/Home/bin/java"));
                output.write(1);
                output.closeEntry();
            }

            DownloadManager manager = new DownloadManager(root.toString(), "jdk") { };
            manager.unzip();

            assertTrue(Files.isRegularFile(root.resolve("jdk/Contents/Home/bin/java")));
            assertFalse(Files.exists(root.resolve(archiveRoot)));
            assertFalse(Files.exists(archive));
        } finally {
            delete(root.toFile());
        }
    }

    private static void delete(File file) throws Exception {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) delete(child);
        }
        Files.deleteIfExists(file.toPath());
    }
}
