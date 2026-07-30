// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.sdk;

import com.totalcross.tooling.platform.StoreLayout;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves the latest stable SDK from Maven metadata and reuses only a digest-verified cache offline. */
public final class SdkVersionCatalog {
  private static final URI METADATA = URI.create("https://maven.totalcross.com/artifactory/repo1/com/totalcross/totalcross-sdk/maven-metadata.xml");
  private static final Pattern VERSION = Pattern.compile("<version>\\s*([0-9]+\\.[0-9]+\\.[0-9]+)\\s*</version>");
  @FunctionalInterface public interface MetadataSource { String read() throws IOException; }

  private final Path cache;
  private final MetadataSource source;

  public SdkVersionCatalog() { this(StoreLayout.detect().cacheRoot().resolve("sdk/maven-metadata.xml"), SdkVersionCatalog::download); }
  SdkVersionCatalog(Path cache, MetadataSource source) { this.cache = cache.toAbsolutePath().normalize(); this.source = source; }

  public String latestStable() throws IOException {
    try {
      String metadata = source.read();
      writeCache(metadata);
      return select(metadata);
    } catch (IOException onlineFailure) {
      try { return select(readCache()); }
      catch (IOException cacheFailure) { onlineFailure.addSuppressed(cacheFailure); throw onlineFailure; }
    }
  }

  static String select(String metadata) throws IOException {
    Matcher matcher = VERSION.matcher(metadata);
    var versions = new java.util.ArrayList<String>();
    while (matcher.find()) versions.add(matcher.group(1));
    return versions.stream().max(SdkVersionCatalog::compare)
        .orElseThrow(() -> new IOException("SDK metadata contains no stable semantic version"));
  }

  private static int compare(String left, String right) {
    String[] leftParts = left.split("\\."), rightParts = right.split("\\.");
    for (int index = 0; index < leftParts.length; index++) {
      int result = Integer.compare(Integer.parseInt(leftParts[index]), Integer.parseInt(rightParts[index]));
      if (result != 0) return result;
    }
    return 0;
  }

  private void writeCache(String metadata) throws IOException {
    Files.createDirectories(cache.getParent());
    Path temporary = cache.resolveSibling(cache.getFileName() + ".part");
    Files.writeString(temporary, metadata, StandardCharsets.UTF_8);
    Files.move(temporary, cache, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    Files.writeString(digestPath(), digest(metadata), StandardCharsets.US_ASCII);
  }

  private String readCache() throws IOException {
    String metadata = Files.readString(cache, StandardCharsets.UTF_8);
    if (!digest(metadata).equals(Files.readString(digestPath(), StandardCharsets.US_ASCII).trim())) {
      throw new IOException("cached SDK metadata digest does not match");
    }
    return metadata;
  }

  private Path digestPath() { return cache.resolveSibling(cache.getFileName() + ".sha256"); }
  private static String download() throws IOException {
    HttpURLConnection connection = (HttpURLConnection) METADATA.toURL().openConnection();
    connection.setConnectTimeout(30_000); connection.setReadTimeout(30_000);
    try {
      if (connection.getResponseCode() / 100 != 2) throw new IOException("SDK metadata download failed: HTTP " + connection.getResponseCode());
      try (var input = connection.getInputStream()) { return new String(input.readAllBytes(), StandardCharsets.UTF_8); }
    } finally { connection.disconnect(); }
  }
  private static String digest(String value) throws IOException {
    try {
      byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(); for (byte valueByte : bytes) hex.append(String.format("%02x", valueByte));
      return hex.toString();
    } catch (NoSuchAlgorithmException unavailable) { throw new IOException("SHA-256 is unavailable", unavailable); }
  }
}
