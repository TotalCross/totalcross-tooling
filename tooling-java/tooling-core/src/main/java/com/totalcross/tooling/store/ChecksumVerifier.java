// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.store;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Computes and verifies SHA-256 digests for downloaded archives. */
public final class ChecksumVerifier {
    private ChecksumVerifier() {
    }

    public static String sha256(Path file) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            input.transferTo(new java.io.OutputStream() {
                @Override public void write(int value) { digest.update((byte) value); }
                @Override public void write(byte[] bytes, int offset, int length) { digest.update(bytes, offset, length); }
            });
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest.digest()) result.append(String.format("%02x", value));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    public static void verify(Path file, String expected) throws IOException {
        String actual = sha256(file);
        if (!actual.equalsIgnoreCase(expected)) {
            throw new IOException("SHA-256 mismatch for " + file + ": expected " + expected + ", got " + actual);
        }
    }
}
