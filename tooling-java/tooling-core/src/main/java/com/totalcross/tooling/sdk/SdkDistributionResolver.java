// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.sdk;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Resolves a complete SDK distribution for deployers that require {@code dist} and {@code etc}. */
public final class SdkDistributionResolver {
  @FunctionalInterface public interface ReleaseLocator { Optional<URI> githubArchive(String version) throws IOException; }
  private final Path cacheRoot;
  private final ReleaseLocator locator;

  public SdkDistributionResolver(Path cacheRoot) { this(cacheRoot, SdkDistributionResolver::githubArchive); }
  SdkDistributionResolver(Path cacheRoot, ReleaseLocator locator) { this.cacheRoot = cacheRoot; this.locator = locator; }

  public Path resolve(String version, Path configuredHome) throws IOException {
    if (configuredHome != null) return validate(configuredHome);
    Path home = cacheRoot.resolve(version);
    if (layout(home)) return prepare(home);
    Files.createDirectories(cacheRoot);
    Path archive = cacheRoot.resolve("TotalCross-" + version + ".zip");
    Path staging = cacheRoot.resolve(version + ".extracting");
    delete(staging);
    if (!Files.isRegularFile(archive)) download(version, archive);
    extract(archive, staging);
    Path root;
    try (var paths = Files.walk(staging)) {
      root = paths.filter(SdkDistributionResolver::layout).findFirst()
          .orElseThrow(() -> new IOException("The TotalCross SDK archive does not contain etc or dist"));
    }
    delete(home);
    move(root, home);
    delete(staging);
    Files.deleteIfExists(archive);
    return prepare(home);
  }

  private void download(String version, Path archive) throws IOException {
    URI fallback = s3Archive(version);
    Optional<URI> github;
    try { github = locator.githubArchive(version); } catch (IOException ignored) { github = Optional.empty(); }
    if (github.isEmpty()) { transfer(fallback, archive); return; }
    try { transfer(github.get(), archive); }
    catch (IOException githubFailure) {
      Files.deleteIfExists(archive);
      try { transfer(fallback, archive); } catch (IOException fallbackFailure) { fallbackFailure.addSuppressed(githubFailure); throw fallbackFailure; }
    }
  }

  private static Optional<URI> githubArchive(String version) throws IOException {
    URI release = URI.create("https://api.github.com/repos/TotalCross/totalcross/releases/tags/v" + version);
    HttpURLConnection connection = (HttpURLConnection) release.toURL().openConnection();
    connection.setConnectTimeout(30_000); connection.setReadTimeout(30_000);
    connection.setRequestProperty("Accept", "application/vnd.github+json");
    connection.setRequestProperty("User-Agent", "totalcross-tooling");
    try {
      int status = connection.getResponseCode();
      if (status == HttpURLConnection.HTTP_NOT_FOUND) return Optional.empty();
      if (status < 200 || status >= 300) throw new IOException("GitHub release lookup failed for " + version + ": HTTP " + status);
      try (InputStream ignored = connection.getInputStream()) { return Optional.of(URI.create("https://github.com/TotalCross/totalcross/releases/download/v" + version + "/TotalCross-" + version + ".zip")); }
    } finally { connection.disconnect(); }
  }

  private static URI s3Archive(String version) { return URI.create("https://totalcross-release.s3.amazonaws.com/" + version.substring(0, Math.min(3, version.length())) + "/TotalCross-" + version + ".zip"); }
  private static void transfer(URI source, Path destination) throws IOException {
    Path temporary = destination.resolveSibling(destination.getFileName() + ".part");
    HttpURLConnection connection = (HttpURLConnection) source.toURL().openConnection();
    connection.setConnectTimeout(30_000); connection.setReadTimeout(120_000);
    try {
      int status = connection.getResponseCode(); if (status < 200 || status >= 300) throw new IOException("Download failed for " + source + ": HTTP " + status);
      try (InputStream input = connection.getInputStream()) { Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING); }
      Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException failure) { Files.deleteIfExists(temporary); throw failure; } finally { connection.disconnect(); }
  }
  private static void extract(Path archive, Path destination) throws IOException {
    Path safe = destination.toAbsolutePath().normalize(); Files.createDirectories(safe);
    try (ZipInputStream input = new ZipInputStream(Files.newInputStream(archive))) {
      for (ZipEntry entry; (entry = input.getNextEntry()) != null; input.closeEntry()) {
        Path target = safe.resolve(entry.getName()).normalize(); if (!target.startsWith(safe)) throw new IOException("Unsafe ZIP entry: " + entry.getName());
        if (entry.isDirectory()) Files.createDirectories(target); else { Files.createDirectories(target.getParent()); Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING); }
      }
    }
  }
  private static Path prepare(Path home) throws IOException {
    validate(home); if (!Files.isDirectory(home.resolve("etc")) && Files.isDirectory(home.resolve("dist"))) {
      Path fonts = home.resolve("etc/fonts"); Files.createDirectories(fonts); Path icons = home.resolve("dist/vm/Material Icons.tcz");
      if (Files.isRegularFile(icons)) Files.copy(icons, fonts.resolve("Material Icons.tcz"), StandardCopyOption.REPLACE_EXISTING);
    } return home;
  }
  private static Path validate(Path home) throws IOException { if (!layout(home)) throw new IOException("TotalCross SDK home must contain etc or dist: " + home); return home.toAbsolutePath().normalize(); }
  private static boolean layout(Path home) { return Files.isDirectory(home.resolve("etc")) || Files.isDirectory(home.resolve("dist")); }
  private static void move(Path source, Path target) throws IOException { try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE); } catch (IOException ignored) { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING); } }
  private static void delete(Path path) throws IOException { if (!Files.exists(path)) return; try (var paths = Files.walk(path)) { for (Path item : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(item); } }
}
