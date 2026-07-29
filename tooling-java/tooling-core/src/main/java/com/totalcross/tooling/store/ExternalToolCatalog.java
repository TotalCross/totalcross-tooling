// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.store;

import com.totalcross.tooling.platform.HostPlatform;
import java.net.URI;
import java.util.Map;
import java.util.Objects;

/** Version/checksum catalog for reusable external deployment tools. */
public final class ExternalToolCatalog {
  public record Entry(String name, String version, String build, String assetName, String payload,
                      String sha256, URI source) {
    public Entry {
      if (sha256 == null || !sha256.matches("[0-9a-fA-F]{64}")) {
        throw new IllegalArgumentException("External tool entries require a concrete SHA-256");
      }
    }
  }

  private static final String PROTOBUF = "https://github.com/protocolbuffers/protobuf/releases/download/v21.0/";
  private static final String BUNDLETOOL = "https://github.com/google/bundletool/releases/download/1.15.6/";
  private static final Entry BUNDLETOOL_ENTRY = new Entry("bundletool", "1.15.6", "all",
      "bundletool-all-1.15.6.jar", "bundletool-all-1.15.6.jar",
      "38ae8a10bcdacef07ecce8211188c5c92b376be96da38ff3ee1f2cf4895b2cb8",
      URI.create(BUNDLETOOL + "bundletool-all-1.15.6.jar"));
  private static final Map<String, Entry> ENTRIES = Map.ofEntries(
      Map.entry("bundletool", BUNDLETOOL_ENTRY),
      Map.entry("protoc:linux-x86_64", protoc("linux-x86_64", "protoc-21.0-linux-x86_64.zip",
          "bin/protoc", "a2a92003da7b8c0c08aab530a3c1967d377c2777723482adb9d2eb38c87a9d5f")),
      Map.entry("protoc:linux-arm64", protoc("linux-arm64", "protoc-21.0-linux-aarch_64.zip",
          "bin/protoc", "72f063d96e4616995dfd24ba2c545ef741b7bf4b25e6077b86f19b41553b79e5")),
      Map.entry("protoc:macos-x86_64", protoc("macos-x86_64", "protoc-21.0-osx-universal_binary.zip",
          "bin/protoc", "e94c66607768c8c47e8864b91e835d50cbe7e18241b34de3c30a4503cc51c36d")),
      Map.entry("protoc:macos-arm64", protoc("macos-arm64", "protoc-21.0-osx-aarch_64.zip",
          "bin/protoc", "4cd865cfe59c18bdae7eaa08f2e18b2ddd29ef8d71602c90ab8ea402c5ba5555")),
      Map.entry("protoc:windows-x86_64", protoc("windows-x86_64", "protoc-21.0-win64.zip",
          "bin/protoc.exe", "2e9e932087722c3b975c704b4b0d91ece9165e3ec0c7b1c035ebb574feec50c2")),
      Map.entry("protoc:windows-arm64", protoc("windows-arm64", "protoc-21.0-win64.zip",
          "bin/protoc.exe", "2e9e932087722c3b975c704b4b0d91ece9165e3ec0c7b1c035ebb574feec50c2")));

  private ExternalToolCatalog() {}
  public static Entry entry(String name) {
    return Objects.equals(name, "bundletool") ? BUNDLETOOL_ENTRY : ENTRIES.get(name + ":" + HostPlatform.detect().id());
  }

  public static Entry entry(String name, HostPlatform platform) {
    return ENTRIES.get(Objects.equals(name, "bundletool") ? "bundletool" : name + ":" + platform.id());
  }

  public static Map<String, Entry> entries() { return ENTRIES; }

  private static Entry protoc(String build, String asset, String payload, String sha256) {
    return new Entry("protoc", "21.0", build, asset, payload, sha256, URI.create(PROTOBUF + asset));
  }
}
