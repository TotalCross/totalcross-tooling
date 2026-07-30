/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.inventory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/** Streams file hashes without following symlinks or inspecting build output. */
public final class ProjectInventoryReader {
  private static final List<String> IGNORED_DIRECTORIES = List.of(
      ".git", ".gradle", "build", "target", "out", "bin", "node_modules", "caches",
      ".totalcross-conversion-backup", ".totalcross-conversion-journal");

  public ProjectInventory read(Path projectRoot) throws IOException {
    Path root = projectRoot.toAbsolutePath().normalize();
    if (!Files.isDirectory(root)) throw new IllegalArgumentException("project directory does not exist: " + root);
    List<ProjectInventory.Entry> entries = new ArrayList<>();
    try {
      Files.walkFileTree(root, new SimpleFileVisitor<>() {
        @Override public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
          if (!directory.equals(root) && IGNORED_DIRECTORIES.contains(directory.getFileName().toString())) {
            return FileVisitResult.SKIP_SUBTREE;
          }
          return FileVisitResult.CONTINUE;
        }

        @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
          if (attributes.isSymbolicLink() || !attributes.isRegularFile()) return FileVisitResult.CONTINUE;
          entries.add(new ProjectInventory.Entry(root.relativize(file), attributes.size(), sha256(file)));
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (SecurityException error) {
      throw new IOException("unable to inventory project: " + root, error);
    }
    entries.sort(java.util.Comparator.comparing(entry -> entry.relativePath().toString()));
    MessageDigest digest = digest();
    for (ProjectInventory.Entry entry : entries) {
      digest.update(entry.relativePath().toString().replace('\\', '/').getBytes(java.nio.charset.StandardCharsets.UTF_8));
      digest.update((byte) 0);
      digest.update(Long.toString(entry.size()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
      digest.update((byte) 0);
      digest.update(entry.sha256().getBytes(java.nio.charset.StandardCharsets.UTF_8));
      digest.update((byte) '\n');
    }
    return new ProjectInventory(root, HexFormat.of().formatHex(digest.digest()), entries);
  }

  private static String sha256(Path file) throws IOException {
    MessageDigest digest = digest();
    try (InputStream input = Files.newInputStream(file)) {
      byte[] buffer = new byte[8192];
      for (int count; (count = input.read(buffer)) >= 0;) digest.update(buffer, 0, count);
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  private static MessageDigest digest() {
    try { return MessageDigest.getInstance("SHA-256"); }
    catch (NoSuchAlgorithmException error) { throw new IllegalStateException("SHA-256 is unavailable", error); }
  }
}
