// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Extracts ZIP or tar.gz archives while rejecting entries that escape the destination. */
public final class ArchiveExtractor {
    public void extract(Path archive, Path destination) throws IOException {
        String name = archive.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (name.endsWith(".zip")) {
            extractZip(archive, destination);
        } else if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
            extractTarGz(archive, destination);
        } else {
            throw new IOException("Unsupported archive type: " + archive.getFileName());
        }
    }

    private static void extractZip(Path archive, Path destination) throws IOException {
        Path root = destination.toAbsolutePath().normalize();
        Files.createDirectories(root);
        try (InputStream raw = Files.newInputStream(archive); ZipInputStream input = new ZipInputStream(raw)) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                Path target = root.resolve(entry.getName()).normalize();
                if (!target.startsWith(root)) throw new IOException("Unsafe ZIP entry: " + entry.getName());
                if (entry.isDirectory()) Files.createDirectories(target);
                else {
                    Files.createDirectories(target.getParent());
                    Files.copy(input, target);
                }
                input.closeEntry();
            }
        }
    }

    private static void extractTarGz(Path archive, Path destination) throws IOException {
        Path root = destination.toAbsolutePath().normalize();
        Files.createDirectories(root);
        try (InputStream raw = Files.newInputStream(archive); InputStream input = new GZIPInputStream(raw)) {
            byte[] header = new byte[512];
            while (readHeader(input, header)) {
                if (isEmpty(header)) return;
                String entryName = field(header, 0, 100);
                String prefix = field(header, 345, 155);
                if (!prefix.isEmpty()) entryName = prefix + "/" + entryName;
                long size = octal(field(header, 124, 12));
                int type = header[156] & 0xff;
                Path target = safeTarget(root, entryName);
                if (type == '5') {
                    Files.createDirectories(target);
                    skip(input, size);
                } else if (type == 0 || type == '0') {
                    Files.createDirectories(target.getParent());
                    copy(input, target, size);
                } else {
                    throw new IOException("Unsupported TAR entry type for " + entryName);
                }
                skip(input, (512 - (size % 512)) % 512);
            }
        }
    }

    private static Path safeTarget(Path root, String name) throws IOException {
        if (name.isBlank()) throw new IOException("Archive entry name is empty");
        Path target = root.resolve(name).normalize();
        if (!target.startsWith(root)) throw new IOException("Unsafe archive entry: " + name);
        return target;
    }

    private static boolean readHeader(InputStream input, byte[] header) throws IOException {
        int offset = 0;
        while (offset < header.length) {
            int count = input.read(header, offset, header.length - offset);
            if (count < 0) return offset != 0 ? failTruncated() : false;
            offset += count;
        }
        return true;
    }

    private static boolean failTruncated() throws IOException {
        throw new IOException("Truncated TAR header");
    }

    private static boolean isEmpty(byte[] value) {
        for (byte item : value) if (item != 0) return false;
        return true;
    }

    private static String field(byte[] value, int offset, int length) {
        int end = offset;
        while (end < offset + length && value[end] != 0) end++;
        return new String(value, offset, end - offset, StandardCharsets.US_ASCII).trim();
    }

    private static long octal(String value) throws IOException {
        try {
            return value.isBlank() ? 0 : Long.parseLong(value.trim(), 8);
        } catch (NumberFormatException error) {
            throw new IOException("Invalid TAR size: " + value, error);
        }
    }

    private static void copy(InputStream input, Path target, long size) throws IOException {
        try (OutputStream output = Files.newOutputStream(target)) {
            byte[] buffer = new byte[8192];
            long remaining = size;
            while (remaining > 0) {
                int count = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (count < 0) throw new IOException("Truncated TAR entry: " + target.getFileName());
                output.write(buffer, 0, count);
                remaining -= count;
            }
        }
    }

    private static void skip(InputStream input, long count) throws IOException {
        long remaining = count;
        while (remaining > 0) {
            long skipped = input.skip(remaining);
            if (skipped > 0) {
                remaining -= skipped;
            } else if (input.read() < 0) {
                throw new IOException("Truncated TAR archive");
            } else {
                remaining--;
            }
        }
    }
}
