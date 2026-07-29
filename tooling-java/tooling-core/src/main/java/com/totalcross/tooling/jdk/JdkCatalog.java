// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

import com.totalcross.tooling.platform.HostPlatform;
import java.util.List;

/** Immutable collection of reviewed JDK archive entries. */
public record JdkCatalog(int schemaVersion, List<JdkCatalogEntry> entries) {
  public static final int SCHEMA_VERSION = 1;

  public JdkCatalog {
    if (schemaVersion != SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported JDK catalog schema: " + schemaVersion);
    entries = List.copyOf(entries == null ? List.of() : entries);
    if (entries.isEmpty()) throw new IllegalArgumentException("JDK catalog has no entries");
    if (entries.stream().map(JdkCatalogEntry::entryId).distinct().count() != entries.size()) throw new IllegalArgumentException("JDK catalog entry IDs must be unique");
  }

  public List<JdkCatalogEntry> candidates(HostPlatform platform, int javaMajor) {
    return entries.stream().filter(entry -> entry.platform().equals(platform) && entry.javaMajor() == javaMajor).toList();
  }
}
