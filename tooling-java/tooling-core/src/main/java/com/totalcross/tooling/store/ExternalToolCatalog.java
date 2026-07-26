// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.store;

import java.util.Map;

/** Version/checksum catalog for reusable external deployment tools. */
public final class ExternalToolCatalog {
  public record Entry(String name, String version, String build, String sha256, String source) {}
  private static final Map<String, Entry> ENTRIES = Map.of(
      "protoc", new Entry("protoc", "21.0", "host", "catalog-required", "https://github.com/protocolbuffers/protobuf/releases/download/v21.0"),
      "bundletool", new Entry("bundletool", "1.15.6", "all", "catalog-required", "https://github.com/google/bundletool/releases/download/1.15.6"));

  private ExternalToolCatalog() {}
  public static Entry entry(String name) { return ENTRIES.get(name); }
  public static Map<String, Entry> entries() { return ENTRIES; }
}
