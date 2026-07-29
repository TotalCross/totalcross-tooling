// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import com.totalcross.tooling.platform.HostPlatform;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Properties;

/** Parses the small versioned properties catalog used by production resolution. */
public final class JdkCatalogParser {
  public JdkCatalog parse(JdkCatalogSource source) throws IOException {
    try (InputStream input = source.open()) { return parse(input); }
  }

  public JdkCatalog parse(InputStream input) throws IOException {
    Properties values = new Properties();
    values.load(input);
    int schema = integer(values, "schemaVersion");
    List<String> ids = values.stringPropertyNames().stream()
        .filter(name -> name.startsWith("entry.") && name.endsWith(".vendor"))
        .map(name -> name.substring("entry.".length(), name.length() - ".vendor".length())).sorted().toList();
    List<JdkCatalogEntry> entries = ids.stream().map(id -> entry(values, schema, id)).toList();
    return new JdkCatalog(schema, entries);
  }

  private static JdkCatalogEntry entry(Properties values, int schema, String id) {
    String prefix = "entry." + id + ".";
    return new JdkCatalogEntry(schema, id, required(values, prefix + "vendor"), integer(values, prefix + "javaMajor"),
        required(values, prefix + "version"), required(values, prefix + "build"),
        HostPlatform.from(required(values, prefix + "operatingSystem"), required(values, prefix + "architecture")),
        JdkArchiveType.parse(required(values, prefix + "archiveType")), URI.create(required(values, prefix + "url")),
        required(values, prefix + "sha256"), required(values, prefix + "javaHomeRelativePath"),
        optionalLong(values.getProperty(prefix + "archiveSize")), values.getProperty(prefix + "releaseDate"));
  }

  private static String required(Properties values, String name) {
    String value = values.getProperty(name);
    if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing JDK catalog field: " + name);
    return value.trim();
  }

  private static int integer(Properties values, String name) {
    try { return Integer.parseInt(required(values, name)); }
    catch (NumberFormatException error) { throw new IllegalArgumentException("Invalid integer JDK catalog field: " + name, error); }
  }

  private static Long optionalLong(String value) {
    if (value == null || value.isBlank()) return null;
    try { return Long.valueOf(value.trim()); }
    catch (NumberFormatException error) { throw new IllegalArgumentException("Invalid JDK catalog archiveSize", error); }
  }
}
